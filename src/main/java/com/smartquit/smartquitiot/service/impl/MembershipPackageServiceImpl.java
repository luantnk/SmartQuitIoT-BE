package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.MembershipPackageDTO;
import com.smartquit.smartquitiot.dto.response.MembershipPackagePlan;
import com.smartquit.smartquitiot.entity.MembershipPackage;
import com.smartquit.smartquitiot.enums.MembershipPackageType;
import com.smartquit.smartquitiot.mapper.MembershipPackageMapper;
import com.smartquit.smartquitiot.repository.MembershipPackageRepository;
import com.smartquit.smartquitiot.service.MembershipPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipPackageServiceImpl implements MembershipPackageService {

    private final MembershipPackageRepository membershipPackageRepository;
    private final MembershipPackageMapper membershipPackageMapper;

    @Override
    public List<MembershipPackageDTO> getMembershipPackages() {
        List<MembershipPackage> membershipPackages = membershipPackageRepository.findAll();

        return membershipPackages.stream().map(membershipPackageMapper::toMembershipPackageDTO).toList();
    }

    @Override
    public List<MembershipPackagePlan> getMembershipPackagesPlanByMembershipPackageId(int membershipPackageId) {
        MembershipPackage membershipPackage = membershipPackageRepository.findById(membershipPackageId)
                .orElseThrow(() -> new RuntimeException("Membership Package Not Found"));

        if(membershipPackage.getType().equals(MembershipPackageType.TRIAL)){
            return List.of();
        }
        List<MembershipPackagePlan> plans = new ArrayList<>();

        MembershipPackagePlan oneMonthPlan = new MembershipPackagePlan();
        oneMonthPlan.setPlanDuration(membershipPackage.getDuration());
        oneMonthPlan.setPlanPrice(membershipPackage.getPrice());
        oneMonthPlan.setPlanDurationUnit(membershipPackage.getDurationUnit().name());
        oneMonthPlan.setMembershipPackage(membershipPackageMapper.toMembershipPackagePlans(membershipPackage));

        MembershipPackagePlan oneYearPlan = new MembershipPackagePlan();
        oneYearPlan.setPlanDuration(membershipPackage.getDuration()*12);
        oneYearPlan.setPlanPrice(membershipPackage.getPrice()*10);
        oneYearPlan.setPlanDurationUnit(membershipPackage.getDurationUnit().name());
        oneYearPlan.setMembershipPackage(membershipPackageMapper.toMembershipPackagePlans(membershipPackage));

        plans.add(oneMonthPlan);
        plans.add(oneYearPlan);

        return plans;
    }
}
