package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Coach;
import com.smartquit.smartquitiot.repository.CoachRepository;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.service.CoachService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CoachServiceImpl implements CoachService {

    private final AccountService accountService;
    private final CoachRepository coachRepository;

    @Override
    public Coach getAuthenticatedCoach() {
        Account authAccount = accountService.getAuthenticatedAccount();
        return coachRepository.findByAccountId(authAccount.getId()).orElseThrow(() -> new RuntimeException("Coach not found"));
    }
}
