package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.InterestCategoryDTO;
import com.smartquit.smartquitiot.entity.InterestCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InterestCategoryMapper {

    public List<InterestCategoryDTO> toListInterestCategoryDTO(List<InterestCategory> interestCategories){
        List<InterestCategoryDTO> listInterestCategoryDTO = new ArrayList<>();
        for (InterestCategory interestCategory : interestCategories) {
            InterestCategoryDTO interestCategoryDTO = new InterestCategoryDTO();
            interestCategoryDTO.setId(interestCategory.getId());
            interestCategoryDTO.setName(interestCategory.getName());
            interestCategoryDTO.setDescription(interestCategory.getDescription());
            listInterestCategoryDTO.add(interestCategoryDTO);
        }
        return listInterestCategoryDTO;
    }
}
