package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.PostCreateRequest;
import com.smartquit.smartquitiot.dto.request.PostUpdateRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.PostDetailDTO;
import com.smartquit.smartquitiot.dto.response.PostSummaryDTO;
import com.smartquit.smartquitiot.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping("/latest")
//    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN', 'COACH')")
    @Operation(summary = "Get latest posts for Home screen", description = "Returns latest N posts with summary info")
    public ResponseEntity<GlobalResponse<List<PostSummaryDTO>>> getLatestPosts(
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<PostSummaryDTO> latestPosts = postService.getLatestPosts(limit);
        return ResponseEntity.ok(GlobalResponse.ok(latestPosts));
    }

    @GetMapping
    //    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN', 'COACH')")

    @Operation(summary = "Get all posts (full list)", description = "Returns all posts with summary info")
    public ResponseEntity<GlobalResponse<List<PostSummaryDTO>>> getAllPosts() {
        List<PostSummaryDTO> allPosts = postService.getAllPosts();
        return ResponseEntity.ok(GlobalResponse.ok(allPosts));
    }

    @GetMapping("/{id}")
    //    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN', 'COACH')")
    @Operation(summary = "Get post detail by id", description = "Returns full post content including media and comments")
    public ResponseEntity<GlobalResponse<PostDetailDTO>> getPostDetail(@PathVariable("id") Integer id) {
        PostDetailDTO postDetail = postService.getPostDetail(id);
        return ResponseEntity.ok(GlobalResponse.ok(postDetail));
    }

    @PostMapping
    public ResponseEntity<PostDetailDTO> createPost(@RequestBody PostCreateRequest request) {
        return ResponseEntity.ok(postService.createPost(request));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostDetailDTO> updatePost(@PathVariable Integer postId, @RequestBody PostUpdateRequest request) {
        return ResponseEntity.ok(postService.updatePost(postId, request));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Integer postId) {
        postService.deletePost(postId);
        return ResponseEntity.ok().body("{\"message\": \"Post deleted successfully\"}");
    }
}
