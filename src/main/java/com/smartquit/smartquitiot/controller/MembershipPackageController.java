package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.MembershipPackageDTO;
import com.smartquit.smartquitiot.service.MembershipPackageService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/membership-packages")
@RequiredArgsConstructor
public class MembershipPackageController {

    private final MembershipPackageService membershipPackageService;

    @GetMapping
    @Operation(summary = "Get all membership package")
    public ResponseEntity<GlobalResponse<List<MembershipPackageDTO>>> getMembershipPackages() {
        return ResponseEntity.ok(
            GlobalResponse.ok("Get Membership Packages", membershipPackageService.getMembershipPackages())
        );
    }

    @GetMapping("/plans/{membershipPackageId}")
    @Operation(summary = "Get all membership package plans by Id", description = "Only Standard and Premium package has more plan")
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
}
