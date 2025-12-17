package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.UpdateReminderTemplateRequest;
import com.smartquit.smartquitiot.dto.response.ReminderTemplateDTO;
import com.smartquit.smartquitiot.entity.ReminderTemplate;
import com.smartquit.smartquitiot.mapper.ReminderTemplateMapper;
import com.smartquit.smartquitiot.repository.ReminderTemplateRepository;
import com.smartquit.smartquitiot.service.ReminderTemplateService;
import com.smartquit.smartquitiot.specifications.ReminderTemplateSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderTemplateServiceImpl implements ReminderTemplateService {
    private final ReminderTemplateRepository reminderTemplateRepository;
    private final ReminderTemplateMapper reminderTemplateMapper;
    @Override
    public Page<ReminderTemplateDTO> getAllReminderTemplate(int page, int size, String search) {
        Pageable pageRequest = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Specification<ReminderTemplate> spec = Specification.allOf(
                ReminderTemplateSpecification.hasSearchString(search)
        );
        Page<ReminderTemplate> reminderTemplates = reminderTemplateRepository.findAll(spec, pageRequest);

        return reminderTemplates.map(reminderTemplateMapper::toReminderTemplateDTO);
    }

    @Override
    public ReminderTemplateDTO updateContent(int id, UpdateReminderTemplateRequest request) {
       ReminderTemplate reminderTemplate = reminderTemplateRepository.findById(id)
               .orElseThrow(() -> new RuntimeException("Reminder Template not found"));
        reminderTemplate.setContent(request.getContent());
        return reminderTemplateMapper.toReminderTemplateDTO(reminderTemplateRepository.save(reminderTemplate));
    }


}
