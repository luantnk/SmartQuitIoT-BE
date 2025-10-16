package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.CommentCreateRequest;
import com.smartquit.smartquitiot.dto.response.PostDetailDTO;

public interface CommentService {
    PostDetailDTO.CommentDTO createComment(Integer postId, CommentCreateRequest request);
}
