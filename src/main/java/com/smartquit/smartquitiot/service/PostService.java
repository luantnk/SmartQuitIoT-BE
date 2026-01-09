package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.PostCreateRequest;
import com.smartquit.smartquitiot.dto.request.PostUpdateRequest;
import com.smartquit.smartquitiot.dto.response.PostDetailDTO;
import com.smartquit.smartquitiot.dto.response.PostSummaryDTO;

import java.util.List;

public interface PostService {
    List<PostSummaryDTO> getLatestPosts(int limit);
    List<PostSummaryDTO> getAllMyPosts();
    List<PostSummaryDTO> getAllPosts(String query);
    PostDetailDTO getPostDetail(Integer postId);
    PostDetailDTO createPost(PostCreateRequest request);
    PostDetailDTO updatePost(Integer postId, PostUpdateRequest request);
    void deletePost(Integer postId);
    void syncAllPosts();
}
