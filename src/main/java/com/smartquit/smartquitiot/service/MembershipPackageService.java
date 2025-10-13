package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.response.MembershipPackageDTO;
import com.smartquit.smartquitiot.dto.response.MembershipPackagePlan;
import com.smartquit.smartquitiot.entity.MembershipPackage;

import java.util.List;

public interface MembershipPackageService {
    List<MembershipPackageDTO> getMembershipPackages();

    List<MembershipPackagePlan>  getMembershipPackagesPlanByMembershipPackageId(int membershipPackageId);
}
