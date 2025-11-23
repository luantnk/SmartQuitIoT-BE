package com.smartquit.smartquitiot.mapper;

import com.smartquit.smartquitiot.dto.response.PaymentDTO;
import com.smartquit.smartquitiot.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentMapper {

    private final MemberMapper memberMapper;
    private final MembershipSubscriptionMapper membershipSubscriptionMapper;

    public PaymentDTO toDTO(Payment payment) {
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setId(payment.getId());
        paymentDTO.setOrderCode(payment.getOrderCode());
        paymentDTO.setPaymentLinkId(payment.getPaymentLinkId());
        paymentDTO.setCreatedAt(payment.getCreatedAt());
        paymentDTO.setAmount(payment.getAmount());
        paymentDTO.setStatus(payment.getStatus());
        paymentDTO.setMember(memberMapper.toMemberPayment(payment.getSubscription().getMember()));
        paymentDTO.setSubscription(membershipSubscriptionMapper.toMembershipSubscriptionPayment(payment.getSubscription()));
        return paymentDTO;
    }
}
