package com.smartquit.smartquitiot.specifications;

import com.smartquit.smartquitiot.entity.Appointment;
import com.smartquit.smartquitiot.enums.AppointmentStatus;
import org.springframework.data.jpa.domain.Specification;

public class AppointmentSpecification {
    public static Specification<Appointment> hasStatus(AppointmentStatus status){
        return (root, query, criteriaBuilder) -> {
            if (status == null || status.name().isEmpty()) {
                return criteriaBuilder.conjunction(); // No filter applied
            }
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("appointmentStatus")), "%" + status.name().toLowerCase() + "%")
            );
        };
    }
}
