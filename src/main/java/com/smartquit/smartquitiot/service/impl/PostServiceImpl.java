package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.AddAchievementRequest;
import com.smartquit.smartquitiot.dto.request.PostCreateRequest;
import com.smartquit.smartquitiot.dto.request.PostUpdateRequest;
import com.smartquit.smartquitiot.dto.response.PostDetailDTO;
import com.smartquit.smartquitiot.dto.response.PostSummaryDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.MediaType;
import com.smartquit.smartquitiot.enums.Role;
import com.smartquit.smartquitiot.mapper.PostMapper;
import com.smartquit.smartquitiot.repository.AccountRepository;
import com.smartquit.smartquitiot.repository.CommentRepository;
import com.smartquit.smartquitiot.repository.PostMediaRepository;
import com.smartquit.smartquitiot.repository.PostRepository;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.repository.*;
import com.smartquit.smartquitiot.service.MemberAchievementService;
import com.smartquit.smartquitiot.service.PostService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final PostMediaRepository postMediaRepository;
    private final CommentRepository commentRepository;
    private final AccountServiceImpl accountService;
    private final MemberAchievementService  memberAchievementService;
    private final MetricRepository metricRepository;


    @Override
    public List<PostSummaryDTO> getLatestPosts(int limit) {
        return postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(PostMapper::toSummaryDTO)
                .collect(Collectors.toList());
    }


    @Override
    public List<PostSummaryDTO> getAllPosts(String query) {
        List<Post> posts;
        if (!StringUtils.hasText(query)) {
            posts = postRepository.findAllByOrderByCreatedAtDesc();
        }
        else {
            posts = postRepository.searchPosts(query.trim());
        }
        return posts.stream()
                .map(PostMapper::toSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PostDetailDTO getPostDetail(Integer postId) {
        Post post = postRepository.findPostWithMediaAndAccount(postId);
        if (post == null) {
            throw new RuntimeException("Post not found with id " + postId);
        }
        List<Comment> rootComments = commentRepository.findRootCommentsByPostId(postId);
        for (Comment c : rootComments) {
            loadRepliesRecursively(c);
        }
//        post.setComments(rootComments);
        return PostMapper.toDetailDTO(post);
    }

    private void loadRepliesRecursively(Comment parent) {
        List<Comment> replies = commentRepository.findRepliesByParentId(parent.getId());
        if (replies.isEmpty()) return;

        for (Comment reply : replies) {
            loadRepliesRecursively(reply);
        }
        parent.setReplies(replies);
    }

    @Transactional
    public void buildCommentTree(List<Comment> comments) {
        for (Comment c : comments) {
            List<Comment> replies = commentRepository.findRepliesByParentId(c.getId());
            c.setReplies(replies);
            buildCommentTree(replies); // đệ quy tiếp
        }
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
        Post savedPost = postRepository.save(post);

        // handle media
        if (request.getMedia() != null && !request.getMedia().isEmpty()) {
            List<PostMedia> mediaList = new ArrayList<>();
            for (PostCreateRequest.PostMediaRequest m : request.getMedia()) {
                if (!StringUtils.hasText(m.getMediaUrl())) continue;

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
        }
        Account  account =  accountService.getAuthenticatedAccount();
        if(account.getRole().equals(Role.MEMBER)){
            Metric metric = metricRepository.findByMemberId(account.getMember().getId())
                    .orElseThrow(() -> new RuntimeException("Metric not found"));
            metric.setPost_count(metric.getPost_count() + 1);
            metricRepository.save(metric);
            AddAchievementRequest addAchievementRequest = new  AddAchievementRequest();
            addAchievementRequest.setField("post_count");
            memberAchievementService.addMemberAchievement(addAchievementRequest).orElse(null);
        }

        return PostMapper.toDTO(savedPost);
    }

    @Override
    @Transactional
    public PostDetailDTO updatePost(Integer postId, PostUpdateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        Account current = accountService.getAuthenticatedAccount();

//        if (post.getAccount().getId() != current.getId() && current.getRole() != Role.ADMIN) {
//            throw new RuntimeException("You do not have permission to edit this post");
//        }


        if (StringUtils.hasText(request.getTitle())) {
            post.setTitle(request.getTitle());
        }
        if (StringUtils.hasText(request.getDescription())) {
            post.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getContent())) {
            post.setContent(request.getContent());
        }
        if (StringUtils.hasText(request.getThumbnail())) {
            post.setThumbnail(request.getThumbnail());
        }

        post.setUpdatedAt(LocalDateTime.now());

        if (request.getMedia() != null) {
            post.getMedia().clear();
            postMediaRepository.deleteAll(post.getMedia());
            List<PostMedia> newMediaList = request.getMedia().stream()
                    .filter(m -> StringUtils.hasText(m.getMediaUrl()))
                    .map(m -> {
                        PostMedia media = new PostMedia();
                        media.setPost(post);
                        media.setMediaUrl(m.getMediaUrl());
                        try {
                            media.setMediaType(MediaType.valueOf(m.getMediaType()));
                        } catch (Exception e) {
                            media.setMediaType(MediaType.IMAGE);
                        }
                        return media;
                    })
                    .collect(Collectors.toList());
            post.getMedia().addAll(newMediaList);
            postMediaRepository.saveAll(newMediaList);
//            post.setMedia(newMediaList);
        }

        Post updatedPost = postRepository.save(post);
        return PostMapper.toDTO(updatedPost);
    }

    @Override
    @Transactional
    public void deletePost(Integer postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));
        postRepository.delete(post);
    }

    @Override
    public List<PostSummaryDTO> getAllMyPosts() {
        Account authAccount = accountService.getAuthenticatedAccount();
        List<Post> myPosts = postRepository.findByAccountId(authAccount.getId());
        return myPosts.stream().map(PostMapper::toSummaryDTO).toList();
    }
}
