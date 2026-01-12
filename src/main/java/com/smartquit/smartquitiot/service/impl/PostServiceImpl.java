package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.document.PostDocument;
import com.smartquit.smartquitiot.dto.request.AddAchievementRequest;
import com.smartquit.smartquitiot.dto.request.PostCreateRequest;
import com.smartquit.smartquitiot.dto.request.PostUpdateRequest;
import com.smartquit.smartquitiot.dto.response.PostDetailDTO;
import com.smartquit.smartquitiot.dto.response.PostSummaryDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.MediaType;
import com.smartquit.smartquitiot.enums.PostStatus;
import com.smartquit.smartquitiot.enums.Role;
import com.smartquit.smartquitiot.mapper.PostMapper;
import com.smartquit.smartquitiot.repository.*;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.service.ContentModerationService;
import com.smartquit.smartquitiot.service.MemberAchievementService;
import com.smartquit.smartquitiot.service.NotificationService;
import com.smartquit.smartquitiot.service.PostService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostSearchRepository postSearchRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final PostMediaRepository postMediaRepository;
    private final CommentRepository commentRepository;
    private final MemberAchievementService memberAchievementService;
    private final MetricRepository metricRepository;
    private final NotificationService notificationService;

    private final ContentModerationService contentModerationService;


    private PostDocument mapToDocument(Post post) {
        Integer accountId = (post.getAccount() != null) ? post.getAccount().getId() : null;
        String username = (post.getAccount() != null) ? post.getAccount().getUsername() : "Unknown";
        return PostDocument.builder()
                .id(post.getId())
                .title(post.getTitle())
                .description(post.getDescription())
                .content(post.getContent())
                .thumbnail(post.getThumbnail())
                .status(post.getStatus().name())
                .createdAt(post.getCreatedAt())
                .accountId(accountId)
                .accountUsername(username)
                .build();
    }

    private PostSummaryDTO mapDocumentToSummaryDTO(PostDocument doc) {
        int commentCount = commentRepository.countByPostId(doc.getId());
        PostSummaryDTO dto = new PostSummaryDTO();
        dto.setId(doc.getId());
        dto.setTitle(doc.getTitle());
        dto.setDescription(doc.getDescription());
        dto.setThumbnail(doc.getThumbnail());
        dto.setCreatedAt(String.valueOf(doc.getCreatedAt()));
        dto.setCommentCount(commentCount);
        return dto;
    }

    private boolean isContentFlagged(String title, String description, String content, String thumbnail) {
        try {
            if (contentModerationService.isTextToxic(title)) return true;
            if (contentModerationService.isTextToxic(description)) return true;
            if (contentModerationService.isTextToxic(content)) return true;

            if (StringUtils.hasText(thumbnail)) {
                if (contentModerationService.isImageNsfw(thumbnail)) return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("AI Service check failed for content. Defaulting to FLAGGED (PENDING_APPROVAL). Error: {}", e.getMessage());
            return true;
        }
    }

    private boolean isMediaFlagged(String url, String mediaType) {
        try {
            if ("VIDEO".equalsIgnoreCase(mediaType)) {
                return contentModerationService.isVideoNsfw(url);
            } else {
                return contentModerationService.isImageNsfw(url);
            }
        } catch (Exception e) {
            log.warn("AI Service check failed for media URL: {}. Defaulting to FLAGGED. Error: {}", url, e.getMessage());
            return true;
        }
    }

    @Override
    public List<PostSummaryDTO> getLatestPosts(int limit) {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
        return posts.stream().map(post -> {
            int commentCount = commentRepository.countByPostId(post.getId());
            return PostMapper.toSummaryDTO(post, commentCount);
        }).toList();
    }

    @Override
    public List<PostSummaryDTO> getAllPosts(String query) {
        if (!StringUtils.hasText(query)) {
            List<Post> posts = postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED);
            return posts.stream().map(post -> {
                int commentCount = commentRepository.countByPostId(post.getId());
                return PostMapper.toSummaryDTO(post, commentCount);
            }).toList();
        } else {
            log.info("Searching posts in ES with query: {}", query);
            List<PostDocument> searchResults = postSearchRepository.searchByKeyword(query);
            return searchResults.stream()
                    .map(this::mapDocumentToSummaryDTO)
                    .toList();
        }
    }

    @Override
    @Transactional
    @Cacheable(value = "post_details", key = "#postId")
    public PostDetailDTO getPostDetail(Integer postId) {
        log.info("------- DB HIT: Fetching Post Detail for ID: {} -------", postId);
        Post post = postRepository.findPostWithMediaAndAccount(postId);
        if (post == null) {
            throw new RuntimeException("Post not found with id " + postId);
        }

        List<Comment> rootComments = commentRepository.findRootCommentsByPostId(postId);
        for (Comment c : rootComments) {
            loadRepliesRecursively(c);
        }
        post.getComments().clear();
        post.getComments().addAll(rootComments);
        int commentCount = commentRepository.countByPostId(post.getId());
        return PostMapper.toDetailDTO(post, commentCount);
    }

    private void loadRepliesRecursively(Comment parent) {
        List<Comment> replies = commentRepository.findRepliesByParentId(parent.getId());
        if (replies.isEmpty()) return;
        for (Comment reply : replies) {
            loadRepliesRecursively(reply);
        }
        parent.setReplies(replies);
    }

    @Override
    @Transactional
    public PostDetailDTO createPost(PostCreateRequest request) {
        if (!StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("Title and content must not be empty");
        }

        Account current = accountService.getAuthenticatedAccount();
        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setDescription(request.getDescription());
        post.setContent(request.getContent());
        post.setThumbnail(request.getThumbnail());
        post.setAccount(current);

        boolean isFlagged = isContentFlagged(request.getTitle(), request.getDescription(), request.getContent(), request.getThumbnail());

        if (isFlagged) {
            post.setStatus(PostStatus.PENDING_APPROVAL);
            log.warn("Post created by {} flagged as TOXIC/NSFW (or AI Error). Status set to PENDING_APPROVAL.", current.getUsername());
        } else {
            post.setStatus(PostStatus.PUBLISHED);
        }

        Post savedPost = postRepository.save(post);

        if (request.getMedia() != null && !request.getMedia().isEmpty()) {
            List<PostMedia> mediaList = new ArrayList<>();
            boolean mediaFlagged = false;

            for (PostCreateRequest.PostMediaRequest m : request.getMedia()) {
                if (!StringUtils.hasText(m.getMediaUrl())) continue;

                if (!isFlagged && !mediaFlagged) {
                    if (isMediaFlagged(m.getMediaUrl(), m.getMediaType())) {
                        mediaFlagged = true;
                    }
                }

                PostMedia media = new PostMedia();
                media.setPost(savedPost);
                media.setMediaUrl(m.getMediaUrl());
                try {
                    media.setMediaType(MediaType.valueOf(m.getMediaType()));
                } catch (Exception e) {
                    media.setMediaType(MediaType.IMAGE);
                }
                mediaList.add(media);
            }
            postMediaRepository.saveAll(mediaList);
            savedPost.setMedia(mediaList);

            if (mediaFlagged && savedPost.getStatus() == PostStatus.PUBLISHED) {
                savedPost.setStatus(PostStatus.PENDING_APPROVAL);
                savedPost = postRepository.save(savedPost);
                log.warn("Post Media flagged as NSFW (or AI Error). Updated Status to PENDING_APPROVAL.");
            }
        }

        if (savedPost.getStatus() == PostStatus.PUBLISHED) {
            try {
                postSearchRepository.save(mapToDocument(savedPost));
            } catch (Exception e) {
                log.error("Failed to sync new post to Elasticsearch: {}", e.getMessage());
            }

            if (current.getRole().equals(Role.MEMBER)) {
                metricRepository.findByMemberId(current.getMember().getId()).ifPresent(metric -> {
                    metric.setPost_count(metric.getPost_count() + 1);
                    metricRepository.save(metric);

                    AddAchievementRequest addAchievementRequest = new AddAchievementRequest();
                    addAchievementRequest.setField("post_count");
                    memberAchievementService.addMemberAchievement(addAchievementRequest);
                });
            }
            notificationService.sendSystemActivityNotification("New post created",
                    "A new post titled '" + savedPost.getTitle() + "' has been created by " + current.getUsername());
        }

        return PostMapper.toDTO(savedPost);
    }

    @Override
    @Transactional
    @CacheEvict(value = "post_details", key = "#postId")
    public PostDetailDTO updatePost(Integer postId, PostUpdateRequest request) {
        log.info("------- CACHE EVICT: Updating Post ID: {} -------", postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        if (StringUtils.hasText(request.getTitle())) post.setTitle(request.getTitle());
        if (StringUtils.hasText(request.getDescription())) post.setDescription(request.getDescription());
        if (StringUtils.hasText(request.getContent())) post.setContent(request.getContent());
        if (StringUtils.hasText(request.getThumbnail())) post.setThumbnail(request.getThumbnail());
        post.setUpdatedAt(LocalDateTime.now());

        boolean isFlagged = isContentFlagged(post.getTitle(), post.getDescription(), post.getContent(), post.getThumbnail());

        if (request.getMedia() != null) {
            post.getMedia().clear();
            postMediaRepository.deleteAll(post.getMedia());
            List<PostMedia> newMediaList = new ArrayList<>();
            for (PostUpdateRequest.PostMediaRequest m : request.getMedia()) {
                if (!StringUtils.hasText(m.getMediaUrl())) continue;

                if (!isFlagged) {
                    if (isMediaFlagged(m.getMediaUrl(), m.getMediaType())) {
                        isFlagged = true;
                    }
                }

                PostMedia media = new PostMedia();
                media.setPost(post);
                media.setMediaUrl(m.getMediaUrl());
                try {
                    media.setMediaType(MediaType.valueOf(m.getMediaType()));
                } catch (Exception e) {
                    media.setMediaType(MediaType.IMAGE);
                }
                newMediaList.add(media);
            }
            post.getMedia().addAll(newMediaList);
            postMediaRepository.saveAll(newMediaList);
        }

        if (isFlagged) {
            post.setStatus(PostStatus.PENDING_APPROVAL);
            try {
                postSearchRepository.deleteById(postId);
            } catch (Exception e) {
                log.error("Failed to remove flagged post from ES: {}", e.getMessage());
            }
        } else {
            post.setStatus(PostStatus.PUBLISHED);
        }

        Post updatedPost = postRepository.save(post);
        if (updatedPost.getStatus() == PostStatus.PUBLISHED) {
            try {
                postSearchRepository.save(mapToDocument(updatedPost));
            } catch (Exception e) {
                log.error("Failed to sync updated post to Elasticsearch: {}", e.getMessage());
            }
        }

        return PostMapper.toDTO(updatedPost);
    }

    @Override
    @Transactional
    @CacheEvict(value = "post_details", key = "#postId")
    public void deletePost(Integer postId) {
        log.info("------- CACHE EVICT: Deleting Post ID: {} -------", postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));
        postRepository.delete(post);
        try {
            postSearchRepository.deleteById(postId);
        } catch (Exception e) {
            log.error("Failed to delete post from Elasticsearch: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void syncAllPosts() {
        log.info("Starting synchronization of all posts to Elasticsearch...");
        List<Post> allPosts = postRepository.findAll();
        List<PostDocument> documents = allPosts.stream()
                .map(this::mapToDocument)
                .collect(Collectors.toList());
        postSearchRepository.saveAll(documents);
        log.info("Successfully synced {} posts to Elasticsearch", documents.size());
    }

    @Override
    public List<PostSummaryDTO> getAllMyPosts() {
        Account authAccount = accountService.getAuthenticatedAccount();
        List<Post> myPosts = postRepository.findByAccountId(authAccount.getId());
        return myPosts.stream().map(post -> {
            int commentCount = commentRepository.countByPostId(post.getId());
            return PostMapper.toSummaryDTO(post, commentCount);
        }).toList();
    }
}