package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.AppointmentRequest;
import com.smartquit.smartquitiot.dto.request.SnapshotUploadRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.JoinTokenResponse;
import com.smartquit.smartquitiot.enums.AppointmentStatus;
import com.smartquit.smartquitiot.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Member: Đặt lịch hẹn với coach",
            description = "Member tạo appointment mới với coach.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> bookAppointment(
            @Valid @RequestBody AppointmentRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            return ResponseEntity.status(401).body(GlobalResponse.error("accountId not found in token", 401));
        }
        int memberAccountId = accountIdNum.intValue();

        var response = appointmentService.bookAppointment(memberAccountId, request);
        return ResponseEntity.ok(GlobalResponse.ok("Booking successful", response));
    }

    @DeleteMapping("/{appointmentId}")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Member: Hủy lịch hẹn đã đặt",
            description = "Member hủy appointment đã tạo. Appointment row sẽ bị xóa.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> cancelByMember(
            @PathVariable int appointmentId,
            @AuthenticationPrincipal Jwt jwt) {

        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            return ResponseEntity.status(401).body(GlobalResponse.error("accountId not found in token", 401));
        }
        int memberAccountId = accountIdNum.intValue();

        appointmentService.cancelAppointment(appointmentId, memberAccountId);
        return ResponseEntity.ok(GlobalResponse.ok("Cancellation successful", null));
    }

    @DeleteMapping("/{appointmentId}/by-coach")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Coach: Hủy lịch hẹn (từ phía coach)",
            description = "Coach có thể hủy appointment do mình phụ trách. Appointment row sẽ bị xóa.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> cancelByCoach(
            @PathVariable int appointmentId,
            @AuthenticationPrincipal Jwt jwt) {

        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            return ResponseEntity.status(401).body(GlobalResponse.error("accountId not found in token", 401));
        }
        int coachAccountId = accountIdNum.intValue();

        appointmentService.cancelAppointmentByCoach(appointmentId, coachAccountId);
        return ResponseEntity.ok(GlobalResponse.ok("Cancellation by coach successful", null));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "Lấy danh sách lịch hẹn cho user hiện tại",
            description = "Nếu user là COACH trả danh sách coach; nếu là MEMBER trả danh sách member. Hỗ trợ filter status/date.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> listForCurrentUser(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "date", required = false) String date, // format: yyyy-MM-dd
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "100") int size,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {

        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            return ResponseEntity.status(401).body(GlobalResponse.error("accountId not found in token", 401));
        }
        int accountId = accountIdNum.intValue();

        boolean isCoach = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_COACH"));

        if (isCoach) {
            var res = appointmentService.getAppointmentsByCoachAccountId(accountId, status, date, page, size);
            return ResponseEntity.ok(GlobalResponse.ok("Coach appointments fetched", res));
        } else {
            var res = appointmentService.getAppointmentsByMemberAccountId(accountId, status, date, page, size);
            return ResponseEntity.ok(GlobalResponse.ok("Member appointments fetched", res));
        }
    }

    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "Chi tiết một lịch hẹn",
            description = "Lấy thông tin chi tiết của appointment; server kiểm tra quyền truy cập.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> getDetail(
            @PathVariable int appointmentId,
            @AuthenticationPrincipal Jwt jwt) {

        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            return ResponseEntity.status(401).body(GlobalResponse.error("accountId not found in token", 401));
        }
        int accountId = accountIdNum.intValue();

        var dto = appointmentService.getAppointmentDetailForPrincipal(appointmentId, accountId);
        return ResponseEntity.ok(GlobalResponse.ok("Appointment detail fetched", dto));
    }

    @PostMapping("/{appointmentId}/join-token")
    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "Tạo join-token Agora cho appointment",
            description = "Member hoặc Coach có thể gọi để nhận token join meeting (server kiểm tra quyền và join window).")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> getJoinToken(
            @PathVariable int appointmentId,
            @AuthenticationPrincipal Jwt jwt) {

        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            return ResponseEntity.status(401).body(GlobalResponse.error("accountId not found in token", 401));
        }
        int accountId = accountIdNum.intValue();

        try {
            JoinTokenResponse dto = appointmentService.generateJoinTokenForAppointment(appointmentId, accountId);
            return ResponseEntity.ok(GlobalResponse.ok("Join token created", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(GlobalResponse.error(e.getMessage(), 404));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(GlobalResponse.error(e.getMessage(), 403));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(GlobalResponse.error(e.getMessage(), 403));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GlobalResponse.error("Error when creating token: " + e.getMessage(), 500));
        }
    }
    @GetMapping("/remaining")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Member: Lấy số lần đặt hẹn còn lại trong kỳ subscription hiện tại",
            description = "Trả số lượt được phép, đã dùng và còn lại. Dựa theo subscription active (startDate..endDate).")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> getRemainingBookings(
            @AuthenticationPrincipal Jwt jwt) {

        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            return ResponseEntity.status(401).body(GlobalResponse.error("accountId not found in token", 401));
        }
        int memberAccountId = accountIdNum.intValue();

        try {
            var dto = appointmentService.getRemainingBookingsForMember(memberAccountId);
            return ResponseEntity.ok(GlobalResponse.ok("Remaining bookings fetched", dto));
        } catch (IllegalArgumentException e) {
            // lỗi do input / không tìm thấy member -> trả 400 (bad request)
            return ResponseEntity.status(400).body(GlobalResponse.error(e.getMessage(), 400));
        } catch (Exception e) {
            // lỗi server khác
            return ResponseEntity.status(500).body(GlobalResponse.error("Error when fetching remaining bookings: " + e.getMessage(), 500));
        }
    }

    @PutMapping("/{appointmentId}/complete")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Coach: Mark appointment completed manually",
            description = "Coach có thể đánh dấu appointment COMPLETED thủ công nếu thỏa điều kiện (start + MIN minutes <= now).")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> completeByCoach(
            @PathVariable int appointmentId,
            @AuthenticationPrincipal Jwt jwt) {

        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            return ResponseEntity.status(401).body(GlobalResponse.error("accountId not found in token", 401));
        }
        int coachAccountId = accountIdNum.intValue();

        try {
            appointmentService.completeAppointmentByCoach(appointmentId, coachAccountId);
            return ResponseEntity.ok(GlobalResponse.ok("Appointment marked as COMPLETED", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(GlobalResponse.error(e.getMessage(), 404));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(GlobalResponse.error(e.getMessage(), 403));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(GlobalResponse.error(e.getMessage(), 403));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(GlobalResponse.error("Error when completing appointment: " + e.getMessage(), 500));
        }
    }
    @PostMapping("/{appointmentId}/snapshots")
    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "Upload snapshot URLs cho appointment",
            description = "Member hoặc Coach upload hình ảnh snapshot sau buổi meeting")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> uploadSnapshots(
            @PathVariable int appointmentId,
            @RequestBody SnapshotUploadRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            return ResponseEntity.status(401)
                    .body(GlobalResponse.error("accountId not found in token", 401));
        }

        appointmentService.addSnapshots(
                appointmentId, accountIdNum.intValue(), request.getImageUrls()
        );

        return ResponseEntity.ok(GlobalResponse.ok("Snapshots uploaded", null));
    }

    @GetMapping("/{appointmentId}/snapshots")
    @PreAuthorize("hasAnyRole('MEMBER','COACH')")
    @Operation(summary = "Lấy danh sách snapshot URLs của appointment",
            description = "Member hoặc Coach có thể xem danh sách ảnh snapshot đã upload.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse> getSnapshots(
            @PathVariable int appointmentId,
            @AuthenticationPrincipal Jwt jwt) {

        Number accountIdNum = jwt.getClaim("accountId");
        if (accountIdNum == null) {
            return ResponseEntity.status(401)
                    .body(GlobalResponse.error("accountId not found in token", 401));
        }

        var urls = appointmentService.getSnapshots(
                appointmentId,
                accountIdNum.intValue()
        );

        return ResponseEntity.ok(
                GlobalResponse.ok("Snapshots fetched", urls)
        );
    }

    @GetMapping("/manage")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy danh sách snapshot URLs của appointment",
            description = "Member hoặc Coach có thể xem danh sách ảnh snapshot đã upload.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> getAllAppointments(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "status", required = false, defaultValue = "") AppointmentStatus status){

        var dtoPage = appointmentService.getAllAppointments(page, size, status);
        return ResponseEntity.ok(dtoPage);
    }

}
