package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.PostDetailDTO;
import com.smartquit.smartquitiot.entity.Comment;
import com.smartquit.smartquitiot.entity.CommentMedia;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CommentMapper {

    private static final DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static PostDetailDTO.CommentDTO toDTO(Comment comment) {
        PostDetailDTO.CommentDTO dto = new PostDetailDTO.CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt() != null ? comment.getCreatedAt().format(fmt) : null);

        // Account
        PostDetailDTO.AccountDTO accDTO = new PostDetailDTO.AccountDTO();
        accDTO.setId(comment.getAccount().getId());
        accDTO.setUsername(comment.getAccount().getUsername());
        dto.setAccount(accDTO);

        // Media
        List<PostDetailDTO.PostMediaDTO> mediaDTOs = new ArrayList<>();
        if (comment.getCommentMedia() != null) {
            for (CommentMedia cm : comment.getCommentMedia()) {
                mediaDTOs.add(new PostDetailDTO.PostMediaDTO(cm.getId(), cm.getMediaUrl(), cm.getMediaType().name()));
            }
        }
        dto.setMedia(mediaDTOs);

        // Replies (recursive)
        List<PostDetailDTO.CommentDTO> replyDTOs = new ArrayList<>();
        if (comment.getReplies() != null) {
            for (Comment reply : comment.getReplies()) {
                replyDTOs.add(toDTO(reply));
            }
        }
        dto.setReplies(replyDTOs);

        return dto;
    }
}
