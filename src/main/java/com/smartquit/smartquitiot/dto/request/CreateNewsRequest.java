package com.smartquit.smartquitiot.dto.request;

import com.smartquit.smartquitiot.enums.NewsStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateNewsRequest {

    String title;
    String content;
    NewsStatus status;
    List<String> mediaUrls;
}
