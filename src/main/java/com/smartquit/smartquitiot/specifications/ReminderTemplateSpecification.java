package com.smartquit.smartquitiot.specifications;

import com.smartquit.smartquitiot.entity.Achievement;
import com.smartquit.smartquitiot.entity.ReminderTemplate;
import org.springframework.data.jpa.domain.Specification;

public class ReminderTemplateSpecification {

    public static Specification<ReminderTemplate> hasSearchString(String searchString){
        return (((root, query, criteriaBuilder) -> {
            if(searchString == null || searchString.isEmpty()){
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.or(criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), "%"+searchString.toLowerCase()+"%"));
        }));
    }
}
