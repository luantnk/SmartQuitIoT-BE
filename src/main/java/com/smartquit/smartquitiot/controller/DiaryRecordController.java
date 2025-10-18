package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.DiaryRecordRequest;
import com.smartquit.smartquitiot.entity.DiaryRecord;
import com.smartquit.smartquitiot.service.DiaryRecordService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/diary-records")
@RequiredArgsConstructor
public class DiaryRecordController {

    private final DiaryRecordService diaryRecordService;

    @PostMapping
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> logDiaryRecord(@RequestBody DiaryRecordRequest request) {
        return ResponseEntity.ok(diaryRecordService.logDiaryRecord(request));
    }

}
