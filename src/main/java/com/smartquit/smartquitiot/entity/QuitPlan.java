package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.QuitPlanStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quit_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuitPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne(fetch = FetchType.LAZY)
    Member member;

    String name;

    @Enumerated(EnumType.STRING)
    QuitPlanStatus status;

    LocalDate startDate;
    LocalDate endDate;

    int ftndScore;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    LocalDateTime updatedAt;

    boolean isActive = true;

    @Column(name = "use_nrt")
    boolean useNRT = false;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "form_metric_id")
    FormMetric formMetric;


    @OneToMany(mappedBy = "quitPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    List<Phase> phases = new ArrayList<>();
}
