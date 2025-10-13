package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.MembershipPackageDTO;
import com.smartquit.smartquitiot.entity.MembershipPackage;
import org.springframework.stereotype.Component;

@Component
public class MembershipPackageMapper {

    public MembershipPackageDTO toMembershipPackageDTO(MembershipPackage membershipPackage) {
        if (membershipPackage == null) {
            return null;
        }
        MembershipPackageDTO membershipPackageDTO = new MembershipPackageDTO();
        membershipPackageDTO.setId(membershipPackage.getId());
        membershipPackageDTO.setName(membershipPackage.getName());
        membershipPackageDTO.setDescription(membershipPackage.getDescription());
        membershipPackageDTO.setPrice(membershipPackage.getPrice());
        membershipPackageDTO.setType(membershipPackage.getType().name());
        membershipPackageDTO.setDuration(membershipPackage.getDuration());
        membershipPackageDTO.setDurationUnit(membershipPackage.getDurationUnit().name());
        membershipPackageDTO.setFeatures(membershipPackage.getFeatures());

        return membershipPackageDTO;
    }

    public MembershipPackageDTO toMembershipPackagePlans(MembershipPackage membershipPackage) {
        if (membershipPackage == null) {
            return null;
        }
        MembershipPackageDTO membershipPackageDTO = new MembershipPackageDTO();
        membershipPackageDTO.setId(membershipPackage.getId());
        membershipPackageDTO.setName(membershipPackage.getName());

        return membershipPackageDTO;
    }
}
