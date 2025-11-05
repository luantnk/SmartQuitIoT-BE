package com.smartquit.smartquitiot.specifications;

import com.smartquit.smartquitiot.entity.MembershipSubscription;
import org.springframework.data.jpa.domain.Specification;

public class MembershipSubscriptionSpecification {
    public static Specification<MembershipSubscription> hasSearchString(Long orderCode) {
        return (root, query, criteriaBuilder) -> {
            if (orderCode == null) {
                return criteriaBuilder.conjunction(); // No filter applied
            }
            return criteriaBuilder.or(
                    criteriaBuilder.like(root.get("orderCode").as(String.class), "%" + orderCode + "%")
            );
        };
    }

    public static Specification<MembershipSubscription> hasStatus(String status){
        return (root, query, criteriaBuilder) -> {
            if (status == null || status.isEmpty()) {
                return criteriaBuilder.conjunction(); // No filter applied
            }
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("status")), "%" + status.toLowerCase() + "%")
            );
        };
    }
}
