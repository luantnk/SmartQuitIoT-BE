    package com.smartquit.smartquitiot.service.impl;

    import com.smartquit.smartquitiot.dto.request.CoachUpdateRequest;
    import com.smartquit.smartquitiot.dto.response.CoachDTO;
    import com.smartquit.smartquitiot.dto.response.CoachSummaryDTO;
    import com.smartquit.smartquitiot.entity.Account;
    import com.smartquit.smartquitiot.entity.Coach;
    import com.smartquit.smartquitiot.mapper.CoachMapper;
    import com.smartquit.smartquitiot.repository.CoachRepository;
    import com.smartquit.smartquitiot.service.AccountService;
    import com.smartquit.smartquitiot.service.CoachService;
    import com.smartquit.smartquitiot.specifications.CoachSpecification;
    import lombok.RequiredArgsConstructor;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.domain.Sort;
    import org.springframework.data.jpa.domain.Specification;
    import org.springframework.stereotype.Service;

    import java.util.List;
    import java.util.Map;

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

        @Override
        public Page<CoachDTO> getAllCoaches(int page, int size, String searchString, Sort.Direction sortBy, Boolean isActive) {
            Specification<Coach> spec = Specification.allOf(CoachSpecification.hasSearchString(searchString)).and(CoachSpecification.hasActive(isActive));
            Pageable pageable = PageRequest.of(page, size,sortBy, "id");
            Page<Coach> coaches = coachRepository.findAll(spec, pageable);
            return coaches.map(coachMapper::toCoachDTO);
        }

        @Override
        public CoachDTO getCoachById(int id) {
            Coach coach = coachRepository.findById(id).orElseThrow(() -> new RuntimeException("Coach not found"));
            return coachMapper.toCoachDTO(coach);
        }

        @Override
        public CoachDTO updateProfile(int coachId, CoachUpdateRequest request) {
            Coach coach = coachRepository.findById(coachId).orElseThrow(() -> new RuntimeException("Coach not found"));
            coach.setFirstName(request.getFirstName());
            coach.setLastName(request.getLastName());
            if(request.getBio() != null) {
                coach.setBio(request.getBio());
            }
            coach.setExperienceYears(request.getExperienceYears());
            if(request.getCertificateUrl() != null){
                coach.setCertificateUrl(request.getCertificateUrl());
            }
            if(request.getAvatarUrl() != null){
                coach.setAvatarUrl(request.getAvatarUrl());
            }
            coach = coachRepository.save(coach);
            return coachMapper.toCoachDTO(coach);
        }

        @Override
        public Map<String, Object> getCoachStatistics() {
            int totalCoaches = coachRepository.findAll().size();
            Map<String, Object> map = Map.of(
                    "totalCoaches", totalCoaches
            );
            return map;
        }
    }
