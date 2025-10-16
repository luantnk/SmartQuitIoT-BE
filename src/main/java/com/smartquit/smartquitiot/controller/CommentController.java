package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.CommentCreateRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.PostDetailDTO;
import com.smartquit.smartquitiot.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/{postId}/comments")
//    @PreAuthorize("hasAnyRole('MEMBER','ADMIN','COACH')")
    @Operation(summary = "Create comment or reply on post", description = "Comment a post or reply to a comment")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse<PostDetailDTO.CommentDTO>> createComment(
            @PathVariable("postId") Integer postId,
            @RequestBody CommentCreateRequest request
    ) {
        PostDetailDTO.CommentDTO created = commentService.createComment(postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.ok(created));
    }
}
