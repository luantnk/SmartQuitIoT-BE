package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.CommentCreateRequest;
import com.smartquit.smartquitiot.dto.response.PostDetailDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.MediaType;
import com.smartquit.smartquitiot.mapper.CommentMapper;
import com.smartquit.smartquitiot.repository.*;
import com.smartquit.smartquitiot.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final PostRepository postRepository;
    private final AccountRepository accountRepository;
    private final CommentRepository commentRepository;
    private final CommentMediaRepository commentMediaRepository;

    @Override
    @Transactional
    public PostDetailDTO.CommentDTO createComment(Integer postId, CommentCreateRequest request) {
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("Content must not be empty");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Account account = accountRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Account not found: " + auth.getName()));

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found: " + request.getParentId()));
            if (parent.getPost().getId() != post.getId()) {
                throw new RuntimeException("Parent comment does not belong to this post");
            }
        }

        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setPost(post);
        comment.setAccount(account);
        comment.setParent(parent);

        Comment saved = commentRepository.save(comment);

        // save media
        if (request.getMedia() != null) {
            List<CommentMedia> mediaList = new ArrayList<>();
            for (CommentCreateRequest.CommentMediaRequest mr : request.getMedia()) {
                if (!StringUtils.hasText(mr.getMediaUrl())) continue;
                CommentMedia cm = new CommentMedia();
                cm.setComment(saved);
                cm.setMediaUrl(mr.getMediaUrl());
                try { cm.setMediaType(MediaType.valueOf(mr.getMediaType())); }
                catch (Exception e){ cm.setMediaType(MediaType.IMAGE);}
                mediaList.add(cm);
            }
            commentMediaRepository.saveAll(mediaList);
            saved.setCommentMedia(mediaList);
        }

        return CommentMapper.toDTO(saved);
    }
}
