package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.entity.InterestCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

public interface InterestCategoryService {
    List<InterestCategory> getAllInterestCategories();
}
