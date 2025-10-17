package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.MembershipSubscriptionDTO;
import com.smartquit.smartquitiot.entity.MembershipSubscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipSubscriptionMapper {

    private final MembershipPackageMapper membershipPackageMapper;

    public MembershipSubscriptionDTO toMembershipSubscriptionDTO(MembershipSubscription membershipSubscription){
        if(membershipSubscription == null) return null;

        MembershipSubscriptionDTO membershipSubscriptionDTO = new MembershipSubscriptionDTO();
        membershipSubscriptionDTO.setId(membershipSubscription.getId());
        membershipSubscriptionDTO.setMembershipPackage(membershipPackageMapper.toMembershipPackageDTO(membershipSubscription.getMembershipPackage()));
        membershipSubscriptionDTO.setStatus(membershipSubscription.getStatus().name());
        membershipSubscriptionDTO.setStartDate(membershipSubscription.getStartDate());
        membershipSubscriptionDTO.setEndDate(membershipSubscription.getEndDate());
        membershipSubscriptionDTO.setCreatedAt(membershipSubscription.getCreatedAt());
        membershipSubscriptionDTO.setOrderCode(membershipSubscription.getOrderCode());
        membershipSubscriptionDTO.setTotalAmount(membershipSubscription.getTotalAmount());

        return membershipSubscriptionDTO;
    }
}
