package com.smartquit.smartquitiot.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailMessageDTO {
    private String to;
    private String subject;
    private String templateName;
    private Map<String, Object> props;
}