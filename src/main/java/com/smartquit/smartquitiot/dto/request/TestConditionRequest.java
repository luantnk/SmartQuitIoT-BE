package com.smartquit.smartquitiot.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartquit.smartquitiot.dto.response.TestData;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestConditionRequest {
     @NotNull(message = "Condition is required")
     JsonNode condition;

     @NotNull(message = "Test data is required")
     TestData testData;
}
