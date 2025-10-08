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
public class CommentMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    @Enumerated(EnumType.STRING)
    MediaType mediaType;
    String mediaUrl;
    @ManyToOne(fetch = FetchType.LAZY)
    Comment comment;
}
