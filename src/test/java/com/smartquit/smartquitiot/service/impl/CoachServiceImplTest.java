package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.CoachUpdateRequest;
import com.smartquit.smartquitiot.dto.response.CoachDTO;
import com.smartquit.smartquitiot.dto.response.CoachSummaryDTO;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Coach;
import com.smartquit.smartquitiot.enums.Gender;
import com.smartquit.smartquitiot.enums.Role;
import com.smartquit.smartquitiot.mapper.CoachMapper;
import com.smartquit.smartquitiot.repository.CoachRepository;
import com.smartquit.smartquitiot.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoachServiceImplTest {

    @Mock
    private AccountService accountService;

    @Mock
    private CoachRepository coachRepository;

    @Mock
    private CoachMapper coachMapper;

    @InjectMocks
    private CoachServiceImpl coachService;

    // Test data
    private Account account;
    private Coach coach;
    private CoachDTO coachDTO;
    private CoachSummaryDTO coachSummaryDTO;

    @BeforeEach
    void setUp() {
        // Setup account
        account = new Account();
        account.setId(100);
        account.setEmail("coach@example.com");
        account.setUsername("coachuser");
        account.setRole(Role.COACH);
        account.setActive(true);
        account.setBanned(false);

        // Setup coach
        coach = new Coach();
        coach.setId(1);
        coach.setFirstName("Jane");
        coach.setLastName("Smith");
        coach.setGender(Gender.FEMALE);
        coach.setBio("Experienced health coach");
        coach.setExperienceYears(5);
        coach.setSpecializations("Health coaching, Wellness");
        coach.setCertificateUrl("http://certificate.url");
        coach.setAvatarUrl("http://avatar.url");
        coach.setAccount(account);
        account.setCoach(coach);

        // Setup DTOs
        coachDTO = new CoachDTO();
        coachDTO.setId(1);
        coachDTO.setFirstName("Jane");
        coachDTO.setLastName("Smith");
        coachDTO.setEmail("coach@example.com");

        coachSummaryDTO = new CoachSummaryDTO();
        coachSummaryDTO.setId(1);
        coachSummaryDTO.setFirstName("Jane");
        coachSummaryDTO.setLastName("Smith");
        coachSummaryDTO.setAccountId(100);
    }

    // ========== getAuthenticatedCoach Tests ==========

    @Test
    void should_get_authenticated_coach_successfully() {
        // ===== GIVEN =====
        when(accountService.getAuthenticatedAccount()).thenReturn(account);
        when(coachRepository.findByAccountId(account.getId()))
                .thenReturn(Optional.of(coach));

        // ===== WHEN =====
        Coach result = coachService.getAuthenticatedCoach();

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(coach.getId());
        assertThat(result.getFirstName()).isEqualTo(coach.getFirstName());
    }

    @Test
    void should_throw_exception_when_coach_not_found_for_authenticated_account() {
        // ===== GIVEN =====
        when(accountService.getAuthenticatedAccount()).thenReturn(account);
        when(coachRepository.findByAccountId(account.getId()))
                .thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> coachService.getAuthenticatedCoach())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Coach not found");
    }

    // ========== getAuthenticatedCoachProfile Tests ==========

    @Test
    void should_get_authenticated_coach_profile_successfully() {
        // ===== GIVEN =====
        when(accountService.getAuthenticatedAccount()).thenReturn(account);
        when(coachRepository.findByAccountId(account.getId()))
                .thenReturn(Optional.of(coach));
        when(coachMapper.toCoachDTO(coach)).thenReturn(coachDTO);

        // ===== WHEN =====
        CoachDTO result = coachService.getAuthenticatedCoachProfile();

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(coachDTO.getId());
        assertThat(result.getFirstName()).isEqualTo(coachDTO.getFirstName());
    }

    // ========== getCoachList Tests ==========

    @Test
    void should_get_list_of_active_coaches_successfully() {
        // ===== GIVEN =====
        List<Coach> coachList = List.of(coach);
        List<CoachSummaryDTO> summaryList = List.of(coachSummaryDTO);

        when(coachRepository.findAllByAccountIsActiveTrueAndAccountIsBannedFalse())
                .thenReturn(coachList);
        when(coachMapper.toCoachSummaryDTO(coachList)).thenReturn(summaryList);

        // ===== WHEN =====
        List<CoachSummaryDTO> result = coachService.getCoachList();

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getId()).isEqualTo(coachSummaryDTO.getId());
    }

    @Test
    void should_return_empty_list_when_no_active_coaches() {
        // ===== GIVEN =====
        when(coachRepository.findAllByAccountIsActiveTrueAndAccountIsBannedFalse())
                .thenReturn(List.of());
        when(coachMapper.toCoachSummaryDTO(anyList())).thenReturn(List.of());

        // ===== WHEN =====
        List<CoachSummaryDTO> result = coachService.getCoachList();

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.isEmpty()).isTrue();
    }

    // ========== getAllCoaches Tests ==========

    @Test
    void should_get_all_coaches_with_pagination_and_filters() {
        // ===== GIVEN =====
        int page = 0;
        int size = 10;
        String searchString = "Jane";
        Sort.Direction sortDirection = Sort.Direction.ASC;
        Boolean isActive = true;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, "id"));
        Page<Coach> coachPage = new PageImpl<>(List.of(coach), pageable, 1);
        Page<CoachDTO> expectedDTOPage = coachPage.map(c -> coachDTO);

        when(coachRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(coachPage);
        when(coachMapper.toCoachDTO(any(Coach.class))).thenReturn(coachDTO);

        // ===== WHEN =====
        Page<CoachDTO> result = coachService.getAllCoaches(page, size, searchString, sortDirection, isActive);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().size()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(coachDTO.getId());
    }

    @Test
    void should_handle_empty_search_results() {
        // ===== GIVEN =====
        int page = 0;
        int size = 10;
        String searchString = "NonExistent";
        Sort.Direction sortDirection = Sort.Direction.ASC;
        Boolean isActive = true;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, "id"));
        Page<Coach> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(coachRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(emptyPage);

        // ===== WHEN =====
        Page<CoachDTO> result = coachService.getAllCoaches(page, size, searchString, sortDirection, isActive);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent().isEmpty()).isTrue();
    }

    // ========== getCoachById Tests ==========

    @Test
    void should_get_coach_by_id_successfully() {
        // ===== GIVEN =====
        int coachId = 1;
        when(coachRepository.findById(coachId)).thenReturn(Optional.of(coach));
        when(coachMapper.toCoachDTO(coach)).thenReturn(coachDTO);

        // ===== WHEN =====
        CoachDTO result = coachService.getCoachById(coachId);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(coachDTO.getId());
        assertThat(result.getFirstName()).isEqualTo(coachDTO.getFirstName());
    }

    @Test
    void should_throw_exception_when_coach_not_found_by_id() {
        // ===== GIVEN =====
        int coachId = 999;
        when(coachRepository.findById(coachId)).thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> coachService.getCoachById(coachId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Coach not found");
    }

    // ========== updateProfile Tests ==========

    @Test
    void should_update_coach_profile_successfully_with_all_fields() {
        // ===== GIVEN =====
        int coachId = 1;
        CoachUpdateRequest request = new CoachUpdateRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");
        request.setBio("Updated bio");
        request.setExperienceYears(10);
        request.setCertificateUrl("http://new-certificate.url");
        request.setAvatarUrl("http://new-avatar.url");
        request.setSpecializations("New specializations");

        Coach updatedCoach = new Coach();
        updatedCoach.setId(coachId);
        updatedCoach.setFirstName(request.getFirstName());
        updatedCoach.setLastName(request.getLastName());
        updatedCoach.setBio(request.getBio());
        updatedCoach.setExperienceYears(request.getExperienceYears());
        updatedCoach.setCertificateUrl(request.getCertificateUrl());
        updatedCoach.setAvatarUrl(request.getAvatarUrl());
        updatedCoach.setAccount(account);

        CoachDTO updatedDTO = new CoachDTO();
        updatedDTO.setId(coachId);
        updatedDTO.setFirstName(request.getFirstName());
        updatedDTO.setLastName(request.getLastName());

        when(coachRepository.findById(coachId)).thenReturn(Optional.of(coach));
        when(coachRepository.save(any(Coach.class))).thenReturn(updatedCoach);
        when(coachMapper.toCoachDTO(any(Coach.class))).thenReturn(updatedDTO);

        // ===== WHEN =====
        CoachDTO result = coachService.updateProfile(coachId, request);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(result.getLastName()).isEqualTo(request.getLastName());

        // Verify coach entity was updated correctly
        assertThat(coach.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(coach.getLastName()).isEqualTo(request.getLastName());
        assertThat(coach.getBio()).isEqualTo(request.getBio());
        assertThat(coach.getExperienceYears()).isEqualTo(request.getExperienceYears());
        assertThat(coach.getCertificateUrl()).isEqualTo(request.getCertificateUrl());
        assertThat(coach.getAvatarUrl()).isEqualTo(request.getAvatarUrl());
    }

    @Test
    void should_update_profile_with_null_optional_fields() {
        // ===== GIVEN =====
        int coachId = 1;
        CoachUpdateRequest request = new CoachUpdateRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");
        request.setExperienceYears(10);
        request.setBio(null);
        request.setCertificateUrl(null);
        request.setAvatarUrl(null);

        // Set initial values
        coach.setBio("Old bio");
        coach.setCertificateUrl("Old cert");
        coach.setAvatarUrl("Old avatar");

        CoachDTO updatedDTO = new CoachDTO();
        updatedDTO.setId(coachId);
        updatedDTO.setFirstName(request.getFirstName());

        when(coachRepository.findById(coachId)).thenReturn(Optional.of(coach));
        when(coachRepository.save(any(Coach.class))).thenReturn(coach);
        when(coachMapper.toCoachDTO(any(Coach.class))).thenReturn(updatedDTO);

        // ===== WHEN =====
        CoachDTO result = coachService.updateProfile(coachId, request);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(coach.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(coach.getLastName()).isEqualTo(request.getLastName());
        assertThat(coach.getExperienceYears()).isEqualTo(request.getExperienceYears());

        // Optional fields should remain unchanged when null
        assertThat(coach.getBio()).isEqualTo("Old bio");
        assertThat(coach.getCertificateUrl()).isEqualTo("Old cert");
        assertThat(coach.getAvatarUrl()).isEqualTo("Old avatar");
    }

    @Test
    void should_throw_exception_when_updating_non_existent_coach() {
        // ===== GIVEN =====
        int coachId = 999;
        CoachUpdateRequest request = new CoachUpdateRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");
        request.setExperienceYears(10);

        when(coachRepository.findById(coachId)).thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> coachService.updateProfile(coachId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Coach not found");

        verify(coachRepository, never()).save(any());
    }

    // ========== getCoachStatistics Tests ==========

    @Test
    void should_get_coach_statistics_successfully() {
        // ===== GIVEN =====
        List<Coach> allCoaches = List.of(coach, new Coach(), new Coach());
        when(coachRepository.findAll()).thenReturn(allCoaches);

        // ===== WHEN =====
        Map<String, Object> result = coachService.getCoachStatistics();

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.get("totalCoaches")).isEqualTo(3);
    }

    @Test
    void should_return_zero_when_no_coaches_exist() {
        // ===== GIVEN =====
        when(coachRepository.findAll()).thenReturn(List.of());

        // ===== WHEN =====
        Map<String, Object> result = coachService.getCoachStatistics();

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.get("totalCoaches")).isEqualTo(0);
    }
}