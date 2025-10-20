package com.smartquit.smartquitiot.service.impl;


import com.smartquit.smartquitiot.dto.response.InterestCategoryDTO;
import com.smartquit.smartquitiot.entity.InterestCategory;
import com.smartquit.smartquitiot.mapper.InterestCategoryMapper;
import com.smartquit.smartquitiot.repository.InterestCategoryRepository;
import com.smartquit.smartquitiot.service.InterestCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterestCategoryServiceImpl implements InterestCategoryService {
    private final InterestCategoryRepository interestCategoryRepository;
    private final InterestCategoryMapper  interestCategoryMapper;
    @Override
    public List<InterestCategoryDTO> getAllInterestCategories() {
        List<InterestCategory> interestCategories = interestCategoryRepository.findAll();
        if(interestCategories.isEmpty()){
            throw new IllegalArgumentException("Interest Category List is Empty");
        }
      return  interestCategoryMapper.toListInterestCategoryDTO(interestCategories);

    }
}
