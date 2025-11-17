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
    public static Specification<Member> hasActive(boolean isActive){
        return (root, query, criteriaBuilder) -> {
            if (isActive) {
                return criteriaBuilder.isTrue(root.get("account").get("isActive"));
            } else {
                return criteriaBuilder.isFalse(root.get("account").get("isActive"));
            }
        };
    }
}
