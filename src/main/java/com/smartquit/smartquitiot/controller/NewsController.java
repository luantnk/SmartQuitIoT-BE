package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.CreateNewsRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.NewsDTO;
import com.smartquit.smartquitiot.enums.NewsStatus;
import com.smartquit.smartquitiot.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.ok(newsService.createNews(request)));

    }

    @GetMapping("/{id}")
    @Operation(summary = "Get news detail by id")
    public ResponseEntity<GlobalResponse<NewsDTO>> getNewsDetail(@PathVariable int id) {
        NewsDTO news = newsService.getNewsDetail(id);
        return ResponseEntity.ok(GlobalResponse.ok(news));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a news item by id")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Void> deleteNews(@PathVariable int id) {
        newsService.deleteNews(id);
        return ResponseEntity.ok().build();

    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a news item")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse<NewsDTO>> updateNews(@PathVariable int id,@RequestBody CreateNewsRequest request) {
        return ResponseEntity.ok(GlobalResponse.ok(newsService.updateNews(id, request)));

    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin get all news with pagination and filters")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Page<NewsDTO>> getAllNewsForAdmin(
            @RequestParam(required = false) NewsStatus status,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        // Parse sort parameters
        Sort.Order order = sort.length > 0 && sort.length >= 2
                ? new Sort.Order(Sort.Direction.fromString(sort[1]), sort[0])
                : Sort.Order.desc("createdAt");

        Pageable pageable = PageRequest.of(page, size, Sort.by(order));

        Page<NewsDTO> newsPage = newsService.getAllNewsWithFilters(status, title, pageable);

        return ResponseEntity.ok(newsPage);
    }


    @PostMapping("/sync")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Sync DB to Elasticsearch", description = "Manually triggers a migration of all old News data into Elasticsearch index")
    public ResponseEntity<?> syncAllNews() {
        newsService.syncAllNews();
        return ResponseEntity.ok("News synchronization started successfully.");
    }
}