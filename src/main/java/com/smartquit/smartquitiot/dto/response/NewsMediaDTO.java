package com.smartquit.smartquitiot.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartquit.smartquitiot.enums.MediaType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NewsMediaDTO {
    Integer id;
    String mediaUrl;
    MediaType mediaType;
}
