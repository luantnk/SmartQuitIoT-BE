package com.smartquit.smartquitiot.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemPhaseCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    String name;

    @Type(JsonType.class)
    @Column(name = "condition_json", columnDefinition = "JSON", nullable = false)
    JsonNode condition;

    @UpdateTimestamp
    @Column(nullable = false)
    LocalDateTime updatedAt;

//    @OneToMany(cascade = CascadeType.ALL, mappedBy = "systemPhaseCondition", orphanRemoval = true)
//    List<Phase> phaseList;

}
