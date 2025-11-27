package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.PaymentProcessRequest;
import com.smartquit.smartquitiot.dto.request.UpdateMembershipPackageRequest;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.MembershipPackageDTO;
import com.smartquit.smartquitiot.dto.response.MembershipPackagePlan;
import com.smartquit.smartquitiot.dto.response.MembershipSubscriptionDTO;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.entity.MembershipPackage;
import com.smartquit.smartquitiot.entity.MembershipSubscription;
import com.smartquit.smartquitiot.entity.Payment;
import com.smartquit.smartquitiot.enums.MembershipPackageType;
import com.smartquit.smartquitiot.enums.MembershipSubscriptionStatus;
import com.smartquit.smartquitiot.enums.PaymentStatus;
import com.smartquit.smartquitiot.mapper.MembershipPackageMapper;
import com.smartquit.smartquitiot.mapper.MembershipSubscriptionMapper;
import com.smartquit.smartquitiot.repository.MemberRepository;
import com.smartquit.smartquitiot.repository.MembershipPackageRepository;
import com.smartquit.smartquitiot.repository.MembershipSubscriptionRepository;
import com.smartquit.smartquitiot.repository.PaymentRepository;
import com.smartquit.smartquitiot.service.EmailService;
import com.smartquit.smartquitiot.service.MemberService;
import com.smartquit.smartquitiot.service.MembershipPackageService;
import com.smartquit.smartquitiot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipPackageServiceImpl implements MembershipPackageService {

    private final MembershipPackageRepository membershipPackageRepository;
    private final MembershipPackageMapper membershipPackageMapper;
    private final MemberService memberService;
    private final EmailService emailService;
    private final MembershipSubscriptionRepository membershipSubscriptionRepository;
    private final MemberRepository memberRepository;
    private final PayOS payOS;
    private final MembershipSubscriptionMapper membershipSubscriptionMapper;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

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
//            Smart Quit Mobile App
            String returnUrl = "smartquit://payment/success";
            String cancelUrl = "smartquit://payment/failed";

            long totalAmount = membershipPackage.getPrice();
            if(duration == 12){
                totalAmount = totalAmount * 10;
            }
            //find old subscription
            Optional<MembershipSubscription> oldSubscription = membershipSubscriptionRepository
                    .findTopByMemberIdAndStatusOrderByCreatedAtDesc(member.getId(), MembershipSubscriptionStatus.AVAILABLE);
            if(oldSubscription.isPresent()){
                long totalAmountOfOldPackage = oldSubscription.get().getTotalAmount();
                LocalDate endDate = oldSubscription.get().getEndDate();
                LocalDate startDate = oldSubscription.get().getStartDate();
                long totalOldSubscriptionDay = ChronoUnit.DAYS.between(startDate, endDate);
                long pricePerDay = totalAmountOfOldPackage/totalOldSubscriptionDay;

                LocalDate now = LocalDate.now();
                long dayRemain = ChronoUnit.DAYS.between(now, endDate);
                long discountMoney = pricePerDay*dayRemain;
                totalAmount = totalAmount - discountMoney;

                if(totalAmount <0) {
                    throw new RuntimeException("Invalid amount. Member can not purchase this Membership Package");
                }
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

    @Override
    public MembershipSubscriptionDTO processMembershipPackagePayment(PaymentProcessRequest request) {
        Member member = memberService.getAuthenticatedMember();
        MembershipSubscription pendingSubscription = membershipSubscriptionRepository.findByOrderCode(request.getOrderCode())
                .orElseThrow(() -> new RuntimeException("Membership Package not found"));
        if(!pendingSubscription.getStatus().equals(MembershipSubscriptionStatus.PENDING)){
            throw new RuntimeException("Invalid pending subscription");
        }
        if(!request.getCancel() && request.getStatus().equals(PaymentStatus.PAID)){
            PaymentLink paymentLink = payOS.paymentRequests().get(request.getId());
            if(!paymentLink.getStatus().equals(PaymentLinkStatus.PAID)){
                throw new RuntimeException("Invalid Payment");
            }
            //inactive old subscription
            Optional<MembershipSubscription> oldSubscription = membershipSubscriptionRepository
                    .findTopByMemberIdAndStatusOrderByCreatedAtDesc(member.getId(), MembershipSubscriptionStatus.AVAILABLE);
            if(oldSubscription.isPresent()){
                MembershipSubscription subscription = oldSubscription.get();
                subscription.setStatus(MembershipSubscriptionStatus.UNAVAILABLE);
                membershipSubscriptionRepository.save(subscription);
            }
            pendingSubscription.setStatus(MembershipSubscriptionStatus.AVAILABLE);
            membershipSubscriptionRepository.save(pendingSubscription);
            Payment payment = new Payment();
            payment.setAmount(pendingSubscription.getTotalAmount());
            payment.setOrderCode(request.getOrderCode());
            payment.setPaymentLinkId(request.getId());
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setSubscription(pendingSubscription);
            paymentRepository.save(payment);
            emailService.sendPaymentSuccessEmail(
                    member.getAccount().getEmail(),
                    member.getAccount().getUsername(),
                    pendingSubscription.getMembershipPackage().getName(),
                    pendingSubscription.getTotalAmount(),
                    request.getOrderCode()
            );
            notificationService.sendSystemActivityNotification("New Membership Subscription",
                    "Member " + member.getAccount().getUsername() + " has successfully subscribed to " +
                            pendingSubscription.getMembershipPackage().getName() + " package.");
        } else {
            if (request.getStatus().equals(PaymentStatus.CANCELLED)) {
                // Khi hủy payment ko lưu thông tin payment, chỉ gửi email cancel payment
                emailService.sendPaymentCancelEmail(
                        member.getAccount().getEmail(),
                        member.getAccount().getUsername(),
                        pendingSubscription.getMembershipPackage().getName(),
                        request.getOrderCode()
                );
            } else {
                pendingSubscription.setStatus(MembershipSubscriptionStatus.UNAVAILABLE);
                membershipSubscriptionRepository.save(pendingSubscription);
            }
        }
        return membershipSubscriptionMapper.toMembershipSubscriptionDTO(pendingSubscription);
    }

    @Override
    public Map<String, Object> getMembershipStatistics() {
        int totalMembershipPackage = membershipPackageRepository.findAll().size();
        int totalSubscriptions = membershipSubscriptionRepository.findAll().size();
        int activeSubscriptions = membershipSubscriptionRepository.countByStatus(MembershipSubscriptionStatus.AVAILABLE);
        List<MembershipSubscription> subscriptions = membershipSubscriptionRepository.findByStatus(MembershipSubscriptionStatus.AVAILABLE);
        Map<MembershipPackage, Long> frequencyMap = subscriptions.stream()
                .collect(Collectors.groupingBy(
                        MembershipSubscription::getMembershipPackage,
                        Collectors.counting()
                ));

        Map.Entry<MembershipPackage, Long> maxEntry = frequencyMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);


        Map<String, Object> response = Map.of(
                "totalMembershipPackage", totalMembershipPackage,
                "totalSubscriptions", totalSubscriptions,
                "activeSubscriptions", activeSubscriptions,
                "mostPopularPackage", maxEntry != null ? maxEntry.getKey().getName() : null
        );

        return response;
    }

    @Override
    public Map<String, Object> getMembershipPackageDetails(int membershipPackageId) {
        MembershipPackage membershipPackage = membershipPackageRepository.findById(membershipPackageId)
                .orElseThrow(() -> new RuntimeException("Membership Package Not Found"));

        List<MembershipPackagePlan> plans = getMembershipPackagesPlanByMembershipPackageId(membershipPackageId);
        return Map.of(
                "membershipPackage", membershipPackageMapper.toMembershipPackageDTO(membershipPackage),
                "plans", plans
        );
    }

    @Override
    public MembershipPackageDTO updateMembershipPackage(UpdateMembershipPackageRequest request) {
        MembershipPackage membershipPackage = membershipPackageRepository.findById(request.getMembershipPackageId())
                .orElseThrow(() -> new RuntimeException("Membership Package Not Found"));
        membershipPackage.setPrice(request.getPrice());
        membershipPackage = membershipPackageRepository.save(membershipPackage);
        return membershipPackageMapper.toMembershipPackageDTO(membershipPackage);
    }
}
