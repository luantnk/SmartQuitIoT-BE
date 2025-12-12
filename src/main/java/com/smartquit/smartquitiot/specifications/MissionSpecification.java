package com.smartquit.smartquitiot.specifications;

import com.smartquit.smartquitiot.entity.Mission;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class MissionSpecification {
    public static Specification<Mission> filterMissions(String search, String status,String phase) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();


            if (search != null && !search.isEmpty()) {
                String likePattern = "%" + search.toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        likePattern
                );
                Predicate codePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("code")),
                        likePattern
                );
                Predicate conditionPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("condition")),
                        likePattern
                );
                predicates.add(criteriaBuilder.or(namePredicate,codePredicate,conditionPredicate));
            }

            // Filter by status
            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (phase != null && !phase.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("phase"), phase));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
