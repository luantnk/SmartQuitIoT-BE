package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.MembershipPackageDTO;
import com.smartquit.smartquitiot.dto.response.MembershipPackagePlan;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.entity.MembershipPackage;
import com.smartquit.smartquitiot.entity.MembershipSubscription;
import com.smartquit.smartquitiot.enums.MembershipPackageType;
import com.smartquit.smartquitiot.enums.MembershipSubscriptionStatus;
import com.smartquit.smartquitiot.mapper.MembershipPackageMapper;
import com.smartquit.smartquitiot.mapper.MembershipSubscriptionMapper;
import com.smartquit.smartquitiot.repository.MemberRepository;
import com.smartquit.smartquitiot.repository.MembershipPackageRepository;
import com.smartquit.smartquitiot.repository.MembershipSubscriptionRepository;
import com.smartquit.smartquitiot.service.MemberService;
import com.smartquit.smartquitiot.service.MembershipPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipPackageServiceImpl implements MembershipPackageService {

    private final MembershipPackageRepository membershipPackageRepository;
    private final MembershipPackageMapper membershipPackageMapper;
    private final MemberService memberService;
    private final MembershipSubscriptionRepository membershipSubscriptionRepository;
    private final MemberRepository memberRepository;
    private final PayOS payOS;
    private final MembershipSubscriptionMapper membershipSubscriptionMapper;

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

    @Transactional
    @Override
    public GlobalResponse<?> createMembershipPackagePayment(int membershipPackageId, int duration) {
        MembershipPackage membershipPackage = membershipPackageRepository.findById(membershipPackageId)
                .orElseThrow(() -> new RuntimeException("Membership Package Not Found"));
        Member member = memberService.getAuthenticatedMember();
        member.getAccount().setFirstLogin(false);
        if(membershipPackage.getType().equals(MembershipPackageType.TRIAL)){
            if(member.isUsedFreeTrial()){
                throw new RuntimeException("Membership Package is used Free Trial");
            }
            MembershipSubscription subscription = new MembershipSubscription();
            subscription.setMembershipPackage(membershipPackage);
            subscription.setMember(member);
            subscription.setStatus(MembershipSubscriptionStatus.AVAILABLE);
            subscription.setStartDate(LocalDate.now());
            subscription.setEndDate(LocalDate.now().plusDays(membershipPackage.getDuration()));
            membershipSubscriptionRepository.save(subscription);


            member.setUsedFreeTrial(true);
            memberRepository.save(member);

            return GlobalResponse.created("Free Trial subscription created", membershipSubscriptionMapper.toMembershipSubscriptionDTO(subscription));
        }else{
            long orderCode =  System.currentTimeMillis() / 1000;
            String returnUrl = "smartquit://payment/success";
            String cancelUrl = "smartquit://payment/failed";

            long totalAmount = membershipPackage.getPrice();
            if(duration == 12){
                totalAmount = totalAmount * 10;
            }
            //create membership subscription pending for payment
            MembershipSubscription subscription = new MembershipSubscription();
            subscription.setMembershipPackage(membershipPackage);
            subscription.setMember(member);
            subscription.setStatus(MembershipSubscriptionStatus.PENDING);//pending for payment success
            subscription.setStartDate(LocalDate.now());
            subscription.setEndDate(LocalDate.now().plusMonths(duration));
            subscription.setOrderCode(orderCode);
            subscription.setTotalAmount(totalAmount);

            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name(membershipPackage.getName())
                    .price(totalAmount)
                    .quantity(1)
                    .build();

            CreatePaymentLinkRequest paymentLinkRequest = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(totalAmount)
                    .description(membershipPackage.getDescription())
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .expiredAt((System.currentTimeMillis() / 1000) + (30 * 60))
                    .item(item)
                    .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(paymentLinkRequest);
            membershipSubscriptionRepository.save(subscription);

            return GlobalResponse.created("Membership subscription payment link created", response);
        }
    }
}
