package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.NewsStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    String title;
    @Column(columnDefinition = "TEXT")
    String content;
    @Enumerated(EnumType.STRING)
    NewsStatus status; // DRAFT, PUBLISHED, DELETED

    String thumbnailUrl;

    @CreationTimestamp
    LocalDateTime createdAt;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL,mappedBy ="news")
    List<NewsMedia> newsMedia;
}
