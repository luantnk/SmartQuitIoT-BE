package com.smartquit.smartquitiot.entity;

import com.smartquit.smartquitiot.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewsMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    String mediaUrl;
    @ManyToOne(fetch = FetchType.LAZY)
    News news;
    @Enumerated(EnumType.STRING)
    MediaType mediaType;
}
