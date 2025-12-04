package com.smartquit.smartquitiot.specifications;

import com.smartquit.smartquitiot.entity.Coach;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public class CoachSpecification {

    public static Specification<Coach> hasSearchString(String searchString) {
        return (root, query, cb) -> {
            if (searchString == null || searchString.isBlank()) {
                return cb.conjunction();
            }

            String pattern = "%" + searchString.trim().toLowerCase() + "%";

            // Example: if Coach has a relation to Account
            // @ManyToOne Account account;
            Join<Object, Object> accountJoin = root.join("account", JoinType.LEFT);

            // firstName, lastName
            Expression<String> firstName = cb.lower(root.get("firstName"));
            Expression<String> lastName = cb.lower(root.get("lastName"));

            // fullName = firstName + " " + lastName
            Expression<String> fullName = cb.lower(
                    cb.concat(
                            cb.concat(root.get("firstName"), " "),
                            root.get("lastName")
                    )
            );

            return cb.or(
                    cb.like(firstName, pattern),
                    cb.like(lastName, pattern),
                    cb.like(fullName, pattern),
                    cb.like(cb.lower(root.get("specializations")), pattern),
                    cb.like(cb.lower(accountJoin.get("username")), pattern),
                    cb.like(cb.lower(accountJoin.get("email")), pattern)
            );
        };
    }
    public static Specification<Coach> hasActive(boolean isActive){
        return (root, query, criteriaBuilder) -> {
            if (isActive) {
                return criteriaBuilder.isTrue(root.get("account").get("isActive"));
            } else {
                return criteriaBuilder.isFalse(root.get("account").get("isActive"));
            }
        };
    }
}
