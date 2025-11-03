package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.CreateNewsRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.NewsDTO;
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
    @Operation(summary = "Get latest news")
    public ResponseEntity<GlobalResponse<List<NewsDTO>>> getLatestNews(
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<NewsDTO> latest = newsService.getLatestNews(limit);
        return ResponseEntity.ok(GlobalResponse.ok(latest));
    }

    @GetMapping
    @Operation(summary = "Get all news with optional search")
    public ResponseEntity<GlobalResponse<List<NewsDTO>>> getAllNews(
            @RequestParam(required = false) String query
    ) {
        List<NewsDTO> news = newsService.getAllNews(query);
        return ResponseEntity.ok(GlobalResponse.ok(news));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a news item")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse<NewsDTO>> createNews(@RequestBody CreateNewsRequest request) {
        return ResponseEntity.ok(GlobalResponse.ok(newsService.createNews(request)));

    }

    @GetMapping("/{id}")
    @Operation(summary = "Get news detail by id")
    public ResponseEntity<GlobalResponse<NewsDTO>> getNewsDetail(@PathVariable int id) {
        NewsDTO news = newsService.getNewsDetail(id);
        return ResponseEntity.ok(GlobalResponse.ok(news));
    }
}
