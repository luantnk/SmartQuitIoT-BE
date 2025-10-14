package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.MembershipPaymentRequest;
import com.smartquit.smartquitiot.dto.request.PaymentProcessRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.MembershipPackageDTO;
import com.smartquit.smartquitiot.dto.response.MembershipSubscriptionDTO;
import com.smartquit.smartquitiot.entity.MembershipSubscription;
import com.smartquit.smartquitiot.service.MembershipPackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.invoices.InvoicesInfo;

import java.util.List;

@RestController
@RequestMapping("/membership-packages")
@RequiredArgsConstructor
public class MembershipPackageController {

    private final MembershipPackageService membershipPackageService;
    private final PayOS payOS;

    @GetMapping
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

//    @GetMapping("/test-payment-link")
//    public ResponseEntity<?> testPayOs(){
//        /*
//        * Payment success url : http://localhost:5173/success?code=00&id=bb812d700c7d4e52b26b50d6a5822440&cancel=false&status=PAID&orderCode=1760344734
//        * Payment failed url : http://localhost:5173/cancel?code=00&id=1cc9fccd2e8d4aad8711b4210380140f&cancel=true&status=CANCELLED&orderCode=1760344840
//        * */
//
//
//        return new ResponseEntity<>(null, HttpStatus.CREATED);
//    }

    //Create payment link if package is free trial will not return payment url
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
    public ResponseEntity<GlobalResponse<MembershipSubscriptionDTO>> processMembershipPayment(@RequestBody PaymentProcessRequest request){
        return ResponseEntity.ok(GlobalResponse.created("Membership payment success", membershipPackageService.processMembershipPackagePayment(request)));
    }

}
