package com.smartquit.smartquitiot.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MemberReminderSettingsRequest {

    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "07:30")
    private LocalTime morningReminderTime;

    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "22:00")
    private LocalTime quietStart;

    @JsonFormat(pattern = "HH:mm")
    @Schema(type = "string", example = "06:00")
    private LocalTime quietEnd;
}
