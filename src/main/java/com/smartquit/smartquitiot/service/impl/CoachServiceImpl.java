    package com.smartquit.smartquitiot.service.impl;

    import com.smartquit.smartquitiot.dto.response.CoachDTO;
    import com.smartquit.smartquitiot.dto.response.CoachSummaryDTO;
    import com.smartquit.smartquitiot.entity.Account;
    import com.smartquit.smartquitiot.entity.Coach;
    import com.smartquit.smartquitiot.mapper.CoachMapper;
    import com.smartquit.smartquitiot.repository.CoachRepository;
    import com.smartquit.smartquitiot.service.AccountService;
    import com.smartquit.smartquitiot.service.CoachService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Service;

    import java.util.List;

    @Service
    @RequiredArgsConstructor
    public class CoachServiceImpl implements CoachService {

        private final AccountService accountService;
        private final CoachRepository coachRepository;
        private final CoachMapper coachMapper;

        @Override
        public Coach getAuthenticatedCoach() {
            Account authAccount = accountService.getAuthenticatedAccount();
            return coachRepository.findByAccountId(authAccount.getId()).orElseThrow(() -> new RuntimeException("Coach not found"));
        }

        @Override
        public CoachDTO getAuthenticatedCoachProfile() {
            Coach coach = getAuthenticatedCoach();
            return coachMapper.toCoachDTO(coach);
        }

        @Override
        public List<CoachSummaryDTO> getCoachList() {
            List<Coach> coachList = coachRepository.findAllByAccountIsActiveTrueAndAccountIsBannedFalse();
            return coachMapper.toCoachSummaryDTO(coachList);
        }
    }
