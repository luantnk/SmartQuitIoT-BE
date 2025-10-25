package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.PostDetailDTO;
import com.smartquit.smartquitiot.dto.response.PostSummaryDTO;
import com.smartquit.smartquitiot.entity.Comment;
import com.smartquit.smartquitiot.entity.Post;
import com.smartquit.smartquitiot.enums.Role;

import java.util.List;
import java.util.stream.Collectors;

public class PostMapper {

    public static PostSummaryDTO toSummaryDTO(Post post) {
        PostSummaryDTO dto = new PostSummaryDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setDescription(post.getDescription());
        dto.setThumbnail(post.getThumbnail());
        dto.setCreatedAt(post.getCreatedAt().toString());

        PostSummaryDTO.AccountDTO accountDTO = new PostSummaryDTO.AccountDTO();
        if (post.getAccount() != null) {
            accountDTO.setId(post.getAccount().getId());
            accountDTO.setUsername(post.getAccount().getUsername());
            if (post.getAccount().getRole().equals(Role.MEMBER)) {
                accountDTO.setFirstName(post.getAccount().getMember().getFirstName());
                accountDTO.setLastName(post.getAccount().getMember().getLastName());
                accountDTO.setAvatarUrl(post.getAccount().getMember().getAvatarUrl());
            }
            if(post.getAccount().getRole().equals(Role.COACH)){
                accountDTO.setFirstName(post.getAccount().getCoach().getFirstName());
                accountDTO.setLastName(post.getAccount().getCoach().getLastName());
                accountDTO.setAvatarUrl(post.getAccount().getCoach().getAvatarUrl());
            }
        }

        dto.setAccount(accountDTO);
        return dto;
    }

    public static PostDetailDTO toDetailDTO(Post post) {
        PostDetailDTO dto = new PostDetailDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setDescription(post.getDescription());
        dto.setThumbnail(post.getThumbnail());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());

        PostDetailDTO.AccountDTO accountDTO = new PostDetailDTO.AccountDTO();
        if (post.getAccount() != null) {
            accountDTO.setId(post.getAccount().getId());
            accountDTO.setUsername(post.getAccount().getUsername());
            accountDTO.setEmail(post.getAccount().getEmail());
            if (post.getAccount().getRole().equals(Role.MEMBER)) {
                accountDTO.setFirstName(post.getAccount().getMember().getFirstName());
                accountDTO.setLastName(post.getAccount().getMember().getLastName());
                accountDTO.setAvatarUrl(post.getAccount().getMember().getAvatarUrl());
            }
            if(post.getAccount().getRole().equals(Role.COACH)){
                accountDTO.setFirstName(post.getAccount().getCoach().getFirstName());
                accountDTO.setLastName(post.getAccount().getCoach().getLastName());
                accountDTO.setAvatarUrl(post.getAccount().getCoach().getAvatarUrl());
            }
        }
        dto.setAccount(accountDTO);

        // media
        if (post.getMedia() != null) {
            List<PostDetailDTO.PostMediaDTO> mediaDTO = post.getMedia().stream()
                    .map(m -> new PostDetailDTO.PostMediaDTO(m.getId(), m.getMediaUrl(), m.getMediaType().name()))
                    .collect(Collectors.toList());
            dto.setMedia(mediaDTO);
        }

        // comments
        if (post.getComments() != null) {
            List<PostDetailDTO.CommentDTO> commentDTOs = post.getComments().stream()
                    .map(PostMapper::toCommentDTO)
                    .collect(Collectors.toList());
            dto.setComments(commentDTOs);
        }

        return dto;
    }


    private static PostDetailDTO.CommentDTO toCommentDTO(Comment comment) {
        PostDetailDTO.CommentDTO dto = new PostDetailDTO.CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt().toString());

        PostDetailDTO.AccountDTO accountDTO = new PostDetailDTO.AccountDTO();
        if (comment.getAccount() != null) {
            accountDTO.setId(comment.getAccount().getId());
            accountDTO.setUsername(comment.getAccount().getUsername());
            if (comment.getAccount().getMember() != null) {
                accountDTO.setFirstName(comment.getAccount().getMember().getFirstName());
                accountDTO.setLastName(comment.getAccount().getMember().getLastName());
                accountDTO.setAvatarUrl(comment.getAccount().getMember().getAvatarUrl());
            }

        }

        dto.setAccount(accountDTO);

        // media
        if (comment.getCommentMedia() != null) {
            List<PostDetailDTO.PostMediaDTO> mediaDTO = comment.getCommentMedia().stream()
                    .map(m -> new PostDetailDTO.PostMediaDTO(m.getId(), m.getMediaUrl(), m.getMediaType().name()))
                    .collect(Collectors.toList());
            dto.setMedia(mediaDTO);
        }

        // replies
        if (comment.getReplies() != null) {
            List<PostDetailDTO.CommentDTO> repliesDTO = comment.getReplies().stream()
                    .map(PostMapper::toCommentDTO)
                    .collect(Collectors.toList());
            dto.setReplies(repliesDTO);
        }

        return dto;
    }

    public static PostDetailDTO toDTO(Post post) {
        if (post == null) return null;

        PostDetailDTO dto = new PostDetailDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setDescription(post.getDescription());
        dto.setContent(post.getContent());
        dto.setThumbnail(post.getThumbnail());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());

        // Account info
        if (post.getAccount() != null) {
            PostDetailDTO.AccountDTO accountDTO = new PostDetailDTO.AccountDTO();
            accountDTO.setId(post.getAccount().getId());
            accountDTO.setUsername(post.getAccount().getUsername());
            accountDTO.setEmail(post.getAccount().getEmail());

            if (post.getAccount().getMember() != null) {
                accountDTO.setFirstName(post.getAccount().getMember().getFirstName());
                accountDTO.setLastName(post.getAccount().getMember().getLastName());
                accountDTO.setAvatarUrl(post.getAccount().getMember().getAvatarUrl());
            }

            dto.setAccount(accountDTO);
        }

        // Media info
        if (post.getMedia() != null && !post.getMedia().isEmpty()) {
            List<PostDetailDTO.PostMediaDTO> mediaDTOs = post.getMedia().stream()
                    .map(media -> {
                        PostDetailDTO.PostMediaDTO mediaDTO = new PostDetailDTO.PostMediaDTO();
                        mediaDTO.setId(media.getId());
                        mediaDTO.setMediaUrl(media.getMediaUrl());
                        mediaDTO.setMediaType(media.getMediaType().name());
                        return mediaDTO;
                    })
                    .collect(Collectors.toList());
            dto.setMedia(mediaDTOs);
        }

        // Comments (nếu muốn load kèm)
        if (post.getComments() != null && !post.getComments().isEmpty()) {
            List<PostDetailDTO.CommentDTO> commentDTOs = post.getComments().stream()
                    .map(PostMapper::toCommentDTO)
                    .collect(Collectors.toList());
            dto.setComments(commentDTOs);
        }

        return dto;
    }


}
