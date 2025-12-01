package com.smartquit.smartquitiot.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SlotReseedRequest {
    
    @NotBlank(message = "Start time is required")
    @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Start time must be in HH:mm format")
    String start;
    
    @NotBlank(message = "End time is required")
    @Pattern(regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", message = "End time must be in HH:mm format")
    String end;
    
    @Min(value = 1, message = "Slot minutes must be greater than 0")
    Integer slotMinutes;
    
    @Min(value = 0, message = "Gap minutes must be 0 or greater")
    Integer gapMinutes;
}

