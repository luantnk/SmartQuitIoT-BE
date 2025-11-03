package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.MembershipSubscriptionDTO;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.entity.MembershipSubscription;
import com.smartquit.smartquitiot.enums.MembershipSubscriptionStatus;
import com.smartquit.smartquitiot.mapper.MembershipSubscriptionMapper;
import com.smartquit.smartquitiot.repository.MembershipSubscriptionRepository;
import com.smartquit.smartquitiot.service.MemberService;
import com.smartquit.smartquitiot.service.MembershipSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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


    @Scheduled(fixedRate = 300000) // Runs every 5 minutes
    private void validateMembershipSubscription(){
        // This method will run every 5 minutes to check for expired subscriptions
        List<MembershipSubscription> activeSubscriptions = membershipSubscriptionRepository
                .findByStatus(MembershipSubscriptionStatus.AVAILABLE);
        for (MembershipSubscription subscription : activeSubscriptions) {
            if(subscription.getEndDate().isAfter(LocalDate.now())){
                subscription.setStatus(MembershipSubscriptionStatus.EXPIRED);
                membershipSubscriptionRepository.save(subscription);
            }
        }

        // This method will run every 5 minutes to check for pending payments older than 10 minutes
        List<MembershipSubscription> pendingPayments = membershipSubscriptionRepository
                .findByStatus(MembershipSubscriptionStatus.PENDING);
        for (MembershipSubscription subscription : pendingPayments) {
            if(subscription.getCreatedAt().plusMinutes(10).isAfter(LocalDateTime.now())){
                subscription.setStatus(MembershipSubscriptionStatus.UNAVAILABLE);
                membershipSubscriptionRepository.save(subscription);
            }
        }
    }
}
