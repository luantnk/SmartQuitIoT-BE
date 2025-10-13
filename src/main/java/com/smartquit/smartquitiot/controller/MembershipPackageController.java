package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.entity.MembershipPackage;
import com.smartquit.smartquitiot.service.MembershipPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/membership-packages")
@RequiredArgsConstructor
public class MembershipPackageController {

    private final MembershipPackageService membershipPackageService;

    @GetMapping
    public ResponseEntity<List<MembershipPackage>> getMembershipPackages() {
        return ResponseEntity.ok(membershipPackageService.getMembershipPackages());
    }
}
