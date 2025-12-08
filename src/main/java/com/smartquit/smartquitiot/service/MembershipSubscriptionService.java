package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.response.MembershipSubscriptionDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MembershipSubscriptionService {

    MembershipSubscriptionDTO getMyMembershipSubscription();

    Page<MembershipSubscriptionDTO> getAllMembershipSubscriptions(
            Integer page,
            Integer size,
            String sortBy,
            String sortDir,
            String orderCode,
            String status
    );

    List<MembershipSubscriptionDTO> getMembershipSubscriptionsByUserId(int memberId);
}
