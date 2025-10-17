package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.CommentCreateRequest;
import com.smartquit.smartquitiot.dto.request.CommentUpdateRequest;
import com.smartquit.smartquitiot.dto.response.PostDetailDTO;

import java.util.List;

public interface CommentService {
    PostDetailDTO.CommentDTO createComment(Integer postId, CommentCreateRequest request);

    PostDetailDTO.CommentDTO updateComment(Integer commentId, CommentUpdateRequest request);

    void deleteComment(Integer commentId);

    List<PostDetailDTO.CommentDTO> getCommentsByPostId(Integer postId);
}
