package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.MembershipPaymentRequest;
import com.smartquit.smartquitiot.dto.request.PaymentProcessRequest;
import com.smartquit.smartquitiot.dto.request.UpdateMembershipPackageRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.MembershipPackageDTO;
import com.smartquit.smartquitiot.dto.response.MembershipSubscriptionDTO;
import com.smartquit.smartquitiot.service.MembershipPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/membership-packages")
@RequiredArgsConstructor
public class MembershipPackageController {

    private final MembershipPackageService membershipPackageService;

    @GetMapping
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get all membership package")
    public ResponseEntity<GlobalResponse<List<MembershipPackageDTO>>> getMembershipPackages() {
        return ResponseEntity.ok(
            GlobalResponse.ok("Get Membership Packages", membershipPackageService.getMembershipPackages())
        );
    }

    @GetMapping("/plans/{membershipPackageId}")
    @Operation(summary = "Get all membership package plans by Id", description = "Chỉ có gói Standard và Premium có plan 1 tháng với 12 tháng")
    public ResponseEntity<?> getMembershipPackagePlans(@PathVariable int membershipPackageId) {
        return ResponseEntity.ok(membershipPackageService.getMembershipPackagesPlanByMembershipPackageId(membershipPackageId));
    }

    @PostMapping("/create-payment-link")
    @Operation(summary = "Create membership payment link",
            description = "Mobile gửi membershipPackageId, duration về, nếu id của gói free trial sẽ không tạo payment link mà sẽ tạo MembershipSubscription với gói Free Trial luôn.")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<?> createMembershipPaymentLink(@RequestBody MembershipPaymentRequest request){
        return new ResponseEntity<>(membershipPackageService
                .createMembershipPackagePayment(request.getMembershipPackageId(), request.getDuration()),
                HttpStatus.CREATED);
    }

    @PostMapping("/process")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Process Payment", description = "Mobile gửi body gồm các trường như bên dưới để BE xác thực thanh toán , xử lí gói membership")
    public ResponseEntity<GlobalResponse<MembershipSubscriptionDTO>> processMembershipPayment(@RequestBody PaymentProcessRequest request){
        return ResponseEntity.ok(GlobalResponse.created("Membership payment success", membershipPackageService.processMembershipPackagePayment(request)));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get membership statistics")
    public ResponseEntity<?> getMembershipStatistics() {
        return ResponseEntity.ok(membershipPackageService.getMembershipStatistics());
    }

    @GetMapping("/detail/{membershipPackageId}")
    @Operation(summary = "Get membership package details by id")
    public ResponseEntity<?> getMembershipPackageDetails(@PathVariable int membershipPackageId) {
        return ResponseEntity.ok(membershipPackageService.getMembershipPackageDetails(membershipPackageId));
    }

    @PutMapping("/update")
    @Operation(summary = "Update membership package")
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalResponse<MembershipPackageDTO>> updateMembershipPackage(@RequestBody UpdateMembershipPackageRequest request){
        return ResponseEntity.ok(GlobalResponse.ok("Update membership package success", membershipPackageService.updateMembershipPackage(request)));
    }

    @GetMapping("/manage")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Get all membership package fot Admin")
    public ResponseEntity<GlobalResponse<List<MembershipPackageDTO>>> manageMembershipPackages() {
        return ResponseEntity.ok(
                GlobalResponse.ok("Get Membership Packages", membershipPackageService.manageMembershipPackages())
        );
    }

}
