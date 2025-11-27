package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.AddAchievementRequest;
import com.smartquit.smartquitiot.dto.request.CommentCreateRequest;
import com.smartquit.smartquitiot.dto.request.CommentUpdateRequest;
import com.smartquit.smartquitiot.dto.response.PostDetailDTO;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.MediaType;
import com.smartquit.smartquitiot.enums.Role;
import com.smartquit.smartquitiot.mapper.CommentMapper;
import com.smartquit.smartquitiot.repository.*;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.service.CommentService;
import com.smartquit.smartquitiot.service.MemberAchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class    CommentServiceImpl implements CommentService {

    private final PostRepository postRepository;
    private final AccountRepository accountRepository;
    private final CommentRepository commentRepository;
    private final CommentMediaRepository commentMediaRepository;
    private final AccountService accountService;
    private final MemberRepository memberRepository;
    private final MetricRepository metricRepository;
    private final MemberAchievementService memberAchievementService;
    private final CoachRepository coachRepository;
    @Override
    @Transactional
    public PostDetailDTO.CommentDTO createComment(Integer postId, CommentCreateRequest request) {
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("Content must not be empty");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        Account account = accountService.getAuthenticatedAccount();
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
                try {
                    cm.setMediaType(MediaType.valueOf(mr.getMediaType()));
                } catch (Exception e) {
                    cm.setMediaType(MediaType.IMAGE);
                }
                mediaList.add(cm);
            }
            commentMediaRepository.saveAll(mediaList);
            saved.setCommentMedia(mediaList);
        }

        if(account.getRole().equals(Role.MEMBER)){
            Metric metric = metricRepository.findByMemberId(account.getMember().getId())
                    .orElseThrow(() -> new RuntimeException("Metric not found"));
            metric.setComment_count(metric.getComment_count() + 1);
            metricRepository.save(metric);
            AddAchievementRequest addAchievementRequest = new  AddAchievementRequest();
            addAchievementRequest.setField("comment_count");
            memberAchievementService.addMemberAchievement(addAchievementRequest).orElse(null);
        }
        return CommentMapper.toDTO(saved);
    }

    @Override
    @Transactional
    public PostDetailDTO.CommentDTO updateComment(Integer commentId, CommentUpdateRequest request) {
        if (!StringUtils.hasText(request.getContent()) && (request.getMedia() == null || request.getMedia().isEmpty())) {
            throw new IllegalArgumentException("Nothing to update");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));

        Account current = accountService.getAuthenticatedAccount();
        if (comment.getAccount().getId() != current.getId()) {
            throw new RuntimeException("You are not allowed to update this comment");
        }


        if (StringUtils.hasText(request.getContent())) {
            comment.setContent(request.getContent());
        }

        if (request.getMedia() != null) {
            comment.getCommentMedia().clear();

//            commentMediaRepository.deleteAll(comment.getCommentMedia());

            List<CommentMedia> mediaList = new ArrayList<>();
            for (CommentCreateRequest.CommentMediaRequest mr : request.getMedia()) {
                if (!StringUtils.hasText(mr.getMediaUrl())) continue;
                CommentMedia cm = new CommentMedia();
                cm.setComment(comment);
                cm.setMediaUrl(mr.getMediaUrl());
                try {
                    cm.setMediaType(MediaType.valueOf(mr.getMediaType()));
                } catch (Exception e) {
                    cm.setMediaType(MediaType.IMAGE);
                }
                mediaList.add(cm);
            }
            comment.getCommentMedia().addAll(mediaList);
            commentMediaRepository.saveAll(mediaList);
        }
        Comment updated = commentRepository.save(comment);
        return CommentMapper.toDTO(updated);
    }

    @Override
    @Transactional
    public void deleteComment(Integer commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));
        Account current = accountService.getAuthenticatedAccount();
        if (comment.getAccount().getId() != current.getId()) {
            throw new RuntimeException("You are not allowed to delete this comment");
        }
        commentRepository.delete(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDetailDTO.CommentDTO> getCommentsByPostId(Integer postId) {
        List<Comment> rootComments = commentRepository.findRootCommentsByPostId(postId);
        for (Comment c : rootComments) {
            loadRepliesRecursively(c);
        }

        List<PostDetailDTO.CommentDTO> commentDTOs = new ArrayList<>();

        for (Comment c : rootComments) {
            Account acc = c.getAccount();
            String avatarUrl = null;

            if (acc.getRole() == Role.MEMBER) {
                Member mem = memberRepository.findByAccountId(acc.getId())
                        .orElseThrow(() -> new RuntimeException("Member not found with account id: " + acc.getId()));
                avatarUrl = mem.getAvatarUrl();
            } else if (acc.getRole() == Role.COACH) {
                Coach coach = coachRepository.findByAccountId(acc.getId())
                        .orElseThrow(() -> new RuntimeException("Coach not found with account id: " + acc.getId()));
                avatarUrl = coach.getAvatarUrl();
            } else if (acc.getRole() == Role.ADMIN) {
                avatarUrl = "https://ui-avatars.com/api/?background=333&color=fff&name=Admin";
            }

            PostDetailDTO.CommentDTO dto = CommentMapper.toDTO(c);
            dto.getAccount().setAvatarUrl(avatarUrl);
            commentDTOs.add(dto);
        }

        return commentDTOs;
    }



    private void loadRepliesRecursively(Comment parent) {
        List<Comment> replies = commentRepository.findRepliesByParentId(parent.getId());
        parent.setReplies(replies);

        for (Comment reply : replies) {
            loadRepliesRecursively(reply);
        }
    }






}
