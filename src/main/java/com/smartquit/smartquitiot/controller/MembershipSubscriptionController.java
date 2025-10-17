package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.MembershipSubscriptionDTO;
import com.smartquit.smartquitiot.service.MembershipSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/membership-subscriptions")
public class MembershipSubscriptionController {

    private final MembershipSubscriptionService membershipSubscriptionService;

    @GetMapping("/current")
    @Operation(summary = "Get current membership package",
            description = "Api này trả về gói membership đang hữu dụng hiện tại")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<GlobalResponse<MembershipSubscriptionDTO>> getCurrentMembershipSubscription(){
        return ResponseEntity.ok(GlobalResponse.ok("Get current membership package success", membershipSubscriptionService.getMyMembershipSubscription()));
    }

}
