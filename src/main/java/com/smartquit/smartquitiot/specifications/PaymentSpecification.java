package com.smartquit.smartquitiot.specifications;

import com.smartquit.smartquitiot.entity.Payment;
import org.springframework.data.jpa.domain.Specification;

public class PaymentSpecification {
    public static Specification<Payment> hasSearchString(String searchString) {
        return (((root, query, criteriaBuilder) -> {
            if(searchString == null || searchString.isEmpty()){
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.concat(
                            criteriaBuilder.concat(root.get("subscription").get("member").get("firstName"), " "), root.get("subscription").get("member").get("lastName")
                    )), "%"+searchString.toLowerCase()+"%"),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("orderCode").as(String.class)), "%"+searchString.toLowerCase()+"%"),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("paymentLinkId")), "%"+searchString.toLowerCase()+"%")
                    );
        }));
    }
}
