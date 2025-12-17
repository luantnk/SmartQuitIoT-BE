package com.smartquit.smartquitiot.service;


import com.smartquit.smartquitiot.dto.request.UpdateReminderTemplateRequest;
import com.smartquit.smartquitiot.dto.response.ReminderTemplateDTO;
import org.springframework.data.domain.Page;

public interface ReminderTemplateService {
    Page<ReminderTemplateDTO> getAllReminderTemplate(int page, int size, String search);
    ReminderTemplateDTO updateContent(int id, UpdateReminderTemplateRequest request);
}
