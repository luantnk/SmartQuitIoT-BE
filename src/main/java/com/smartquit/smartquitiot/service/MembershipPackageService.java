package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.PaymentProcessRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.MembershipPackageDTO;
import com.smartquit.smartquitiot.dto.response.MembershipPackagePlan;
import com.smartquit.smartquitiot.dto.response.MembershipSubscriptionDTO;

import java.util.List;

public interface MembershipPackageService {
    List<MembershipPackageDTO> getMembershipPackages();

    List<MembershipPackagePlan>  getMembershipPackagesPlanByMembershipPackageId(int membershipPackageId);

    GlobalResponse<?> createMembershipPackagePayment(int membershipPackageId, int duration);

    MembershipSubscriptionDTO processMembershipPackagePayment(PaymentProcessRequest request);
}
