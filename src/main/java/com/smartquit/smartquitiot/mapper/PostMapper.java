package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.PostDetailDTO;
import com.smartquit.smartquitiot.dto.response.PostSummaryDTO;
import com.smartquit.smartquitiot.entity.Comment;
import com.smartquit.smartquitiot.entity.Post;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PostMapper {

    public static PostSummaryDTO toSummaryDTO(Post post, int commentCount) {

        AccountMapper accountMapper = new AccountMapper();

        PostSummaryDTO dto = new PostSummaryDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setDescription(post.getDescription());
        dto.setThumbnail(post.getThumbnail());
        dto.setCreatedAt(post.getCreatedAt().toString());
        if(!post.getMedia().isEmpty()) {
            dto.setMediaUrl(post.getMedia().getFirst().getMediaUrl());
        }
        dto.setAccount(accountMapper.toAccountPostDTO(post.getAccount()));
        dto.setCommentCount(commentCount);
        return dto;
    }

    public static PostDetailDTO toDetailDTO(Post post, int commentCount) {
        AccountMapper accountMapper = new AccountMapper();
        PostDetailDTO dto = new PostDetailDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setDescription(post.getDescription());
        dto.setThumbnail(post.getThumbnail());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());
        dto.setCommentCount(commentCount);

        dto.setAccount(accountMapper.toAccountPostDTO(post.getAccount()));

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

        AccountMapper accountMapper = new AccountMapper();

        dto.setAccount(accountMapper.toAccountPostDTO(comment.getAccount()));

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
            AccountMapper accountMapper = new AccountMapper();

            dto.setAccount(accountMapper.toAccountPostDTO(post.getAccount()));
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
