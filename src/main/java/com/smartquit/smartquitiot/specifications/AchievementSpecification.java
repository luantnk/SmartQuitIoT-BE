package com.smartquit.smartquitiot.specifications;

import com.smartquit.smartquitiot.entity.Achievement;
import org.springframework.data.jpa.domain.Specification;

public class AchievementSpecification {

    public static Specification<Achievement> hasSearchString(String searchString){
        return (((root, query, criteriaBuilder) -> {
            if(searchString == null || searchString.isEmpty()){
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.or(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%"+searchString.toLowerCase()+"%"),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%"+searchString.toLowerCase()+"%"));
        }));
    }
}
