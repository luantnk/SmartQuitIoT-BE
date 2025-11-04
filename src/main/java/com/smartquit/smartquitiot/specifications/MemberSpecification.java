package com.smartquit.smartquitiot.specifications;

import com.smartquit.smartquitiot.entity.Member;
import org.springframework.data.jpa.domain.Specification;

public class MemberSpecification {

    public static Specification<Member> hasSearchString(String searchString) {
        return (((root, query, criteriaBuilder) -> {
            if(searchString == null || searchString.isEmpty()){
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.or(criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), "%"+searchString.toLowerCase()+"%"),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), "%"+searchString.toLowerCase()+"%"));
        }));
    }
}
