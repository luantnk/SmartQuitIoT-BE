package com.smartquit.smartquitiot.service.impl;


import com.smartquit.smartquitiot.entity.InterestCategory;
import com.smartquit.smartquitiot.repository.InterestCategoryRepository;
import com.smartquit.smartquitiot.service.InterestCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterestCategoryServiceImpl implements InterestCategoryService {
    private final InterestCategoryRepository interestCategoryRepository;

    @Override
    public List<InterestCategory> getAllInterestCategories() {
        return interestCategoryRepository.findAll();
    }
}
