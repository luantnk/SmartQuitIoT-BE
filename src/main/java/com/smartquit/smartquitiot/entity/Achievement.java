package com.smartquit.smartquitiot.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartquit.smartquitiot.enums.AchievementType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.Type;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    String name;
    String description;
    String icon;
    @Type(JsonType.class)
    @Column(name = "condition_json", columnDefinition = "JSON", nullable = false)
    JsonNode condition;
    @Enumerated(EnumType.STRING)
    AchievementType type;
}
