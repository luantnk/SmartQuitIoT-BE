package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.MembershipSubscriptionDTO;
import com.smartquit.smartquitiot.entity.MembershipSubscription;
import org.springframework.stereotype.Component;

@Component
public class MembershipSubscriptionMapper {

    public MembershipSubscriptionDTO toMembershipSubscriptionDTO(MembershipSubscription membershipSubscription){
        MembershipSubscriptionDTO membershipSubscriptionDTO = new MembershipSubscriptionDTO();
        membershipSubscriptionDTO.setId(membershipSubscription.getId());
        membershipSubscriptionDTO.setMembershipPackageId(membershipSubscription.getMembershipPackage().getId());
        membershipSubscriptionDTO.setStatus(membershipSubscription.getStatus().name());
        membershipSubscriptionDTO.setStartDate(membershipSubscription.getStartDate());
        membershipSubscriptionDTO.setEndDate(membershipSubscription.getEndDate());
        membershipSubscriptionDTO.setCreatedAt(membershipSubscription.getCreatedAt());
        membershipSubscriptionDTO.setOrderCode(membershipSubscription.getOrderCode());
        membershipSubscriptionDTO.setTotalAmount(membershipSubscription.getTotalAmount());

        return membershipSubscriptionDTO;
    }
}
