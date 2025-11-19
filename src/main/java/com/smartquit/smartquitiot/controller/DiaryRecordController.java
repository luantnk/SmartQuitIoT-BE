package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.DiaryRecordRequest;
import com.smartquit.smartquitiot.entity.DiaryRecord;
import com.smartquit.smartquitiot.service.DiaryRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/diary-records")
@RequiredArgsConstructor
public class DiaryRecordController {

    private final DiaryRecordService diaryRecordService;

    @PostMapping("/log")
    @PreAuthorize("hasRole('MEMBER')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> logDiaryRecord(@RequestBody @Valid DiaryRecordRequest request) {
        return ResponseEntity.ok(diaryRecordService.logDiaryRecord(request));
    }

    @GetMapping("/history")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get diary record history for the authenticated member",
            description = "Chỉ có member đang đăng nhập mới có thể lấy được lịch sử nhật ký của chính họ.")
    public ResponseEntity<?> getDiaryRecordHistory() {
        return ResponseEntity.ok(diaryRecordService.getDiaryRecordsForMember());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get diary record detail")
    public ResponseEntity<?> getDiaryRecordById(@PathVariable Integer id) {
        return ResponseEntity.ok(diaryRecordService.getDiaryRecordById(id));
    }

    @GetMapping("/charts")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get diary record chart for the authenticated member",
            description = "Chỉ có member đang đăng nhập mới có thể lấy được biểu đồ.")
    public ResponseEntity<?> getDiaryRecordsCharts() {
        return ResponseEntity.ok(diaryRecordService.getDiaryRecordsCharts());
    }

    @GetMapping("/charts/{memberId}")
    @Operation(summary = "Get diary record chart for member",
            description = "API để lấy biểu đồ nhật ký của member")
    public ResponseEntity<?> getDiaryRecordsChartsByMemberId(@PathVariable int memberId) {
        return ResponseEntity.ok(diaryRecordService.getDiaryRecordsChartsByMemberId(memberId));
    }

    @GetMapping("/history/{memberId}")
    @Operation(summary = "Get diary record history for member",
            description = "API để lấy lịch sử nhật ký của member")
    public ResponseEntity<?> getDiaryRecordHistoryByMemberId(@PathVariable int memberId) {
        return ResponseEntity.ok(diaryRecordService.getDiaryRecordsHistoryByMemberId(memberId));
    }

    @GetMapping("/check-today")
    @PreAuthorize("hasRole('MEMBER')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Check if the authenticated member has created a diary record today",
            description = "Trả về true nếu hôm nay đã tạo record, false nếu chưa.")
    public ResponseEntity<?> hasCreatedDiaryRecordToday() {
        boolean hasRecord = diaryRecordService.hasCreatedDiaryRecordToday();
        return ResponseEntity.ok(Map.of("hasRecordToday", hasRecord));
    }


}
