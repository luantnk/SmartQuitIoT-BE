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
public class PostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    @ManyToOne(fetch = FetchType.LAZY)
    Post post;
    String mediaUrl;
    @Enumerated(EnumType.STRING)
    MediaType mediaType;
}
