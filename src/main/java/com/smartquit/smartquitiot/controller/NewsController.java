package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.NewsResponseDTO;
import com.smartquit.smartquitiot.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/latest")
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get latest news")
    public ResponseEntity<GlobalResponse<List<NewsResponseDTO>>> getLatestNews(
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<NewsResponseDTO> latest = newsService.getLatestNews(limit);
        return ResponseEntity.ok(GlobalResponse.ok(latest));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEMBER', 'ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get all news with optional search")
    public ResponseEntity<GlobalResponse<List<NewsResponseDTO>>> getAllNews(
            @RequestParam(required = false) String query
    ) {
        List<NewsResponseDTO> news = newsService.getAllNews(query);
        return ResponseEntity.ok(GlobalResponse.ok(news));
    }
}
