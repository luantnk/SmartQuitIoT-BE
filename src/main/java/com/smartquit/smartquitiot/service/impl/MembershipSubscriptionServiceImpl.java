package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.PaymentProcessRequest;
import com.smartquit.smartquitiot.dto.response.MembershipSubscriptionDTO;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.entity.MembershipSubscription;
import com.smartquit.smartquitiot.enums.MembershipSubscriptionStatus;
import com.smartquit.smartquitiot.mapper.MembershipSubscriptionMapper;
import com.smartquit.smartquitiot.repository.MembershipSubscriptionRepository;
import com.smartquit.smartquitiot.service.MemberService;
import com.smartquit.smartquitiot.service.MembershipSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MembershipSubscriptionServiceImpl implements MembershipSubscriptionService {

    private final MemberService memberService;
    private final MembershipSubscriptionRepository membershipSubscriptionRepository;
    private final MembershipSubscriptionMapper membershipSubscriptionMapper;

    @Override
    public MembershipSubscriptionDTO getMyMembershipSubscription() {
        Member member = memberService.getAuthenticatedMember();
        MembershipSubscription currentSubscription = membershipSubscriptionRepository
                .findTopByMemberIdAndStatusOrderByCreatedAtDesc(member.getId(), MembershipSubscriptionStatus.AVAILABLE).orElse(null);
        return membershipSubscriptionMapper.toMembershipSubscriptionDTO(currentSubscription);
    }

}
