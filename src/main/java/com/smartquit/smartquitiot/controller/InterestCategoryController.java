package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.response.InterestCategoryDTO;
import com.smartquit.smartquitiot.service.InterestCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/interest-category")
@RequiredArgsConstructor
public class InterestCategoryController {
    private final InterestCategoryService interestCategoryService;

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','MEMBER','COACH')")
    @Operation(summary = "Get all Interest category")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<InterestCategoryDTO>> getAllInterestCategories() {
        return ResponseEntity.ok(interestCategoryService.getAllInterestCategories());
    }
}
