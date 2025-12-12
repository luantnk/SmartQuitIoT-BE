package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.PaymentProcessRequest;
import com.smartquit.smartquitiot.dto.request.UpdateMembershipPackageRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.MembershipPackageDTO;
import com.smartquit.smartquitiot.dto.response.MembershipPackagePlan;
import com.smartquit.smartquitiot.dto.response.MembershipSubscriptionDTO;

import java.util.List;
import java.util.Map;

public interface MembershipPackageService {
    List<MembershipPackageDTO> getMembershipPackages();

    List<MembershipPackagePlan>  getMembershipPackagesPlanByMembershipPackageId(int membershipPackageId);

    GlobalResponse<?> createMembershipPackagePayment(int membershipPackageId, int duration);

    MembershipSubscriptionDTO processMembershipPackagePayment(PaymentProcessRequest request);

    Map<String, Object> getMembershipStatistics();

    Map<String, Object> getMembershipPackageDetails(int membershipPackageId);

    MembershipPackageDTO updateMembershipPackage(UpdateMembershipPackageRequest request);

    List<MembershipPackageDTO> manageMembershipPackages();
}
