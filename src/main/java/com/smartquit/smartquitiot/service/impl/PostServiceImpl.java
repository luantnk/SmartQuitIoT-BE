package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.PostCreateRequest;
import com.smartquit.smartquitiot.dto.request.PostUpdateRequest;
import com.smartquit.smartquitiot.dto.response.PostDetailDTO;
import com.smartquit.smartquitiot.dto.response.PostSummaryDTO;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Post;
import com.smartquit.smartquitiot.entity.PostMedia;
import com.smartquit.smartquitiot.enums.MediaType;
import com.smartquit.smartquitiot.mapper.PostMapper;
import com.smartquit.smartquitiot.repository.AccountRepository;
import com.smartquit.smartquitiot.repository.PostMediaRepository;
import com.smartquit.smartquitiot.repository.PostRepository;
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
    private final PostMediaRepository postMediaRepository;

    @Override
    public List<PostSummaryDTO> getLatestPosts(int limit) {
        return postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream()
                .map(PostMapper::toSummaryDTO)
                .collect(Collectors.toList());
    }

//    @Override
//    public List<PostSummaryDTO> getAllPosts() {
//        return postRepository.findAllByOrderByCreatedAtDesc()
//                .stream()
//                .map(PostMapper::toSummaryDTO)
//                .collect(Collectors.toList());
//    }

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
    public PostDetailDTO getPostDetail(Integer postId) {
        Post post = postRepository.findPostDetailById(postId);
        return PostMapper.toDetailDTO(post);
    }

    @Override
    @Transactional
    public PostDetailDTO createPost(PostCreateRequest request) {

        System.out.println("Received request to create post: " + request.toString());

        if (!StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("Title and content must not be empty");
        }

        if (request.getAccountId() == null) {
            throw new IllegalArgumentException("Account ID is required");
        }

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found: " + request.getAccountId()));

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setDescription(request.getDescription());
        post.setContent(request.getContent());
        post.setThumbnail(request.getThumbnail());
        post.setAccount(account);

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

        return PostMapper.toDTO(savedPost);
    }

    @Override
    @Transactional
    public PostDetailDTO updatePost(Integer postId, PostUpdateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        if (request.getAccountId() == null) {
            throw new IllegalArgumentException("Account ID is required for update");
        }

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + request.getAccountId()));

        if (post.getAccount().getId() != account.getId() &&
                (account.getRole() == null || !account.getRole().name().equals("ADMIN"))) {
            throw new RuntimeException("You do not have permission to edit this post");
        }

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
            // Xóa media cũ
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

            postMediaRepository.saveAll(newMediaList);
            post.setMedia(newMediaList);
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


}
