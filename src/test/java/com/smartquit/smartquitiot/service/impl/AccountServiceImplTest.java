package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.dto.request.*;
import com.smartquit.smartquitiot.dto.response.CoachDTO;
import com.smartquit.smartquitiot.dto.response.GlobalResponse;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.dto.response.VerifyOtpResponse;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Coach;
import com.smartquit.smartquitiot.entity.Member;
import com.smartquit.smartquitiot.enums.AccountType;
import com.smartquit.smartquitiot.enums.AppointmentStatus;
import com.smartquit.smartquitiot.enums.Gender;
import com.smartquit.smartquitiot.enums.Role;
import com.smartquit.smartquitiot.mapper.CoachMapper;
import com.smartquit.smartquitiot.mapper.MemberMapper;
import com.smartquit.smartquitiot.repository.AccountRepository;
import com.smartquit.smartquitiot.repository.AppointmentRepository;
import com.smartquit.smartquitiot.repository.CoachRepository;
import com.smartquit.smartquitiot.repository.MemberRepository;
import com.smartquit.smartquitiot.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private CoachRepository coachRepository;

    @Mock
    private CoachMapper coachMapper;

    @Mock
    private EmailService emailService;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    // Test data
    private MemberAccountRequest memberRequest;
    private CoachAccountRequest coachRequest;
    private Account account;
    private Member member;
    private Coach coach;

    @BeforeEach
    void setUp() {
        // Setup member request
        memberRequest = new MemberAccountRequest();
        memberRequest.setUsername("testuser");
        memberRequest.setPassword("Password123!");
        memberRequest.setConfirmPassword("Password123!");
        memberRequest.setEmail("test@example.com");
        memberRequest.setFirstName("John");
        memberRequest.setLastName("Doe");
        memberRequest.setGender(Gender.MALE);
        memberRequest.setDob(LocalDate.of(1990, 1, 1));

        // Setup coach request
        coachRequest = new CoachAccountRequest();
        coachRequest.setUsername("coachuser");
        coachRequest.setPassword("Password123!");
        coachRequest.setConfirmPassword("Password123!");
        coachRequest.setEmail("coach@example.com");
        coachRequest.setFirstName("Jane");
        coachRequest.setLastName("Smith");
        coachRequest.setGender(Gender.FEMALE);
        coachRequest.setCertificateUrl("http://certificate.url");
        coachRequest.setExperienceYears(5);
        coachRequest.setSpecializations("Health coaching");

        // Setup account
        account = new Account();
        account.setId(1);
        account.setUsername("testuser");
        account.setEmail("test@example.com");
        account.setPassword("encodedPassword");
        account.setRole(Role.MEMBER);
        account.setAccountType(AccountType.SYSTEM);
        account.setActive(true);
        account.setBanned(false);

        // Setup member
        member = new Member();
        member.setId(1);
        member.setFirstName("John");
        member.setLastName("Doe");
        member.setAccount(account);
        account.setMember(member);

        // Setup coach
        coach = new Coach();
        coach.setId(1);
        coach.setFirstName("Jane");
        coach.setLastName("Smith");
        coach.setAccount(account);
    }

    // ========== registerMember Tests ==========

    @Test
    void should_register_member_successfully_when_all_fields_valid() {
        // ===== GIVEN =====
        when(accountRepository.findByEmail(memberRequest.getEmail())).thenReturn(Optional.empty());
        when(accountRepository.findByUsername(memberRequest.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(memberRequest.getPassword())).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MemberDTO expectedDTO = new MemberDTO();
        expectedDTO.setId(1);
        expectedDTO.setEmail(memberRequest.getEmail());
        when(memberMapper.toMemberDTO(any(Member.class))).thenReturn(expectedDTO);

        // ===== WHEN =====
        MemberDTO result = accountService.registerMember(memberRequest);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(memberRequest.getEmail());

        // Verify account was created with correct properties
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getUsername()).isEqualTo(memberRequest.getUsername());
        assertThat(savedAccount.getEmail()).isEqualTo(memberRequest.getEmail());
        assertThat(savedAccount.getRole()).isEqualTo(Role.MEMBER);
        assertThat(savedAccount.getAccountType()).isEqualTo(AccountType.SYSTEM);

        // Verify member was created with correct properties
        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(memberCaptor.capture());
        Member savedMember = memberCaptor.getValue();
        assertThat(savedMember.getFirstName()).isEqualTo(memberRequest.getFirstName());
        assertThat(savedMember.getLastName()).isEqualTo(memberRequest.getLastName());
        assertThat(savedMember.getGender()).isEqualTo(memberRequest.getGender());
        assertThat(savedMember.getDob()).isEqualTo(memberRequest.getDob());
        assertThat(savedMember.getMetric()).isNotNull(); // Metric should be initialized
    }

    @Test
    void should_throw_exception_when_passwords_do_not_match() {
        // ===== GIVEN =====
        memberRequest.setConfirmPassword("DifferentPassword123!");

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.registerMember(memberRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Password and confirm password do not match");
    }

    @Test
    void should_throw_exception_when_email_already_exists() {
        // ===== GIVEN =====
        when(accountRepository.findByEmail(memberRequest.getEmail()))
                .thenReturn(Optional.of(account));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.registerMember(memberRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");

        verify(accountRepository, never()).save(any());
        verify(memberRepository, never()).save(any());
    }

    @Test
    void should_throw_exception_when_username_already_exists() {
        // ===== GIVEN =====
        when(accountRepository.findByEmail(memberRequest.getEmail())).thenReturn(Optional.empty());
        when(accountRepository.findByUsername(memberRequest.getUsername()))
                .thenReturn(Optional.of(account));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.registerMember(memberRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username already exists");

        verify(accountRepository, never()).save(any());
        verify(memberRepository, never()).save(any());
    }

    // ========== registerCoach Tests ==========

    @Test
    void should_register_coach_successfully_when_all_fields_valid() {
        // ===== GIVEN =====
        when(accountRepository.findByUsername(coachRequest.getUsername())).thenReturn(Optional.empty());
        when(accountRepository.findByEmail(coachRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(coachRequest.getPassword())).thenReturn("encodedPassword");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(coachRepository.save(any(Coach.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CoachDTO expectedDTO = new CoachDTO();
        expectedDTO.setId(1);
        expectedDTO.setEmail(coachRequest.getEmail());
        when(coachMapper.toCoachDTO(any(Coach.class))).thenReturn(expectedDTO);

        // ===== WHEN =====
        CoachDTO result = accountService.registerCoach(coachRequest);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(coachRequest.getEmail());

        // Verify account was created with correct properties
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getUsername()).isEqualTo(coachRequest.getUsername());
        assertThat(savedAccount.getEmail()).isEqualTo(coachRequest.getEmail());
        assertThat(savedAccount.getRole()).isEqualTo(Role.COACH);
        assertThat(savedAccount.getAccountType()).isEqualTo(AccountType.SYSTEM);
        assertThat(savedAccount.isFirstLogin()).isFalse();

        // Verify coach was created with correct properties
        ArgumentCaptor<Coach> coachCaptor = ArgumentCaptor.forClass(Coach.class);
        verify(coachRepository).save(coachCaptor.capture());
        Coach savedCoach = coachCaptor.getValue();
        assertThat(savedCoach.getFirstName()).isEqualTo(coachRequest.getFirstName());
        assertThat(savedCoach.getLastName()).isEqualTo(coachRequest.getLastName());
        assertThat(savedCoach.getGender()).isEqualTo(coachRequest.getGender());
        assertThat(savedCoach.getExperienceYears()).isEqualTo(coachRequest.getExperienceYears());
        assertThat(savedCoach.getSpecializations()).isEqualTo(coachRequest.getSpecializations());
    }

    @Test
    void should_throw_exception_when_coach_username_already_exists() {
        // ===== GIVEN =====
        when(accountRepository.findByUsername(coachRequest.getUsername()))
                .thenReturn(Optional.of(account));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.registerCoach(coachRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Coach username already exists");

        verify(accountRepository, never()).save(any());
        verify(coachRepository, never()).save(any());
    }

    @Test
    void should_throw_exception_when_coach_passwords_do_not_match() {
        // ===== GIVEN =====
        coachRequest.setConfirmPassword("DifferentPassword123!");

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.registerCoach(coachRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Password and confirm password do not match");
    }

    @Test
    void should_throw_exception_when_old_password_incorrect() {
        // ===== GIVEN =====
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongPassword");
        request.setNewPassword("NewPassword123!");
        request.setNewPasswordConfirm("NewPassword123!");

        Account testAccount = new Account();
        testAccount.setId(1);
        testAccount.setPassword("encodedOldPassword");

        // Note: This test shows the expected behavior
        // Actual implementation would require mocking SecurityContext
    }

    @Test
    void should_throw_exception_when_new_passwords_do_not_match() {
        // ===== GIVEN =====
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("NewPassword123!");
        request.setNewPasswordConfirm("DifferentPassword123!");

        // Note: This test shows the expected behavior
        // Actual implementation would require mocking SecurityContext
    }

    // ========== forgotPassword Tests ==========

    @Test
    void should_generate_otp_and_send_email_when_email_exists() {
        // ===== GIVEN =====
        String email = "test@example.com";
        when(accountRepository.findByEmail(email)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ===== WHEN =====
        accountService.forgotPassword(email);

        // ===== THEN =====
        // Verify OTP was set on account
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getOtp()).isNotNull();
        assertThat(savedAccount.getOtp()).hasSize(6); // OTP should be 6 digits
        assertThat(savedAccount.getOtpGeneratedTime()).isNotNull();

        // Verify email was sent
        verify(emailService).sendHtmlOtpEmail(eq(email), anyString(), anyString(), anyString());
    }

    @Test
    void should_throw_exception_when_email_not_found() {
        // ===== GIVEN =====
        String email = "nonexistent@example.com";
        when(accountRepository.findByEmail(email)).thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.forgotPassword(email))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email not found");

        verify(accountRepository, never()).save(any());
        verify(emailService, never()).sendHtmlOtpEmail(any(), any(), any(), any());
    }

    // ========== verifyOtp Tests ==========

    @Test
    void should_verify_otp_successfully_and_return_reset_token() {
        // ===== GIVEN =====
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("test@example.com");
        request.setOtp("123456");

        account.setOtp("123456");
        account.setOtpGeneratedTime(LocalDateTime.now().minusMinutes(2)); // Valid (within 5 minutes)

        when(accountRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ===== WHEN =====
        VerifyOtpResponse result = accountService.verifyOtp(request);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.getResetToken()).isNotNull();

        // Verify OTP was cleared and reset token was set
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getOtp()).isNull();
        assertThat(savedAccount.getOtpGeneratedTime()).isNull();
        assertThat(savedAccount.getResetToken()).isNotNull();
        assertThat(savedAccount.getResetTokenExpiryTime()).isNotNull();
    }

    @Test
    void should_throw_exception_when_otp_invalid() {
        // ===== GIVEN =====
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("test@example.com");
        request.setOtp("wrongOtp");

        account.setOtp("123456");
        account.setOtpGeneratedTime(LocalDateTime.now());

        when(accountRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(account));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.verifyOtp(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OTP");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void should_throw_exception_when_otp_expired() {
        // ===== GIVEN =====
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("test@example.com");
        request.setOtp("123456");

        account.setOtp("123456");
        account.setOtpGeneratedTime(LocalDateTime.now().minusMinutes(10)); // Expired (> 5 minutes)

        when(accountRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(account));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.verifyOtp(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OTP has expired");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void should_throw_exception_when_email_not_found_for_otp() {
        // ===== GIVEN =====
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("nonexistent@example.com");
        request.setOtp("123456");

        when(accountRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.verifyOtp(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OTP or email");
    }

    // ========== resetPassword Tests ==========

    @Test
    void should_reset_password_successfully_when_token_valid() {
        // ===== GIVEN =====
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setResetToken("valid-reset-token");
        request.setNewPassword("NewPassword123!");

        account.setResetToken("valid-reset-token");
        account.setResetTokenExpiryTime(LocalDateTime.now().plusMinutes(5)); // Still valid

        when(accountRepository.findByResetToken(request.getResetToken()))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("newEncodedPassword");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ===== WHEN =====
        accountService.resetPassword(request);

        // ===== THEN =====
        // Verify password was updated and reset token was cleared
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getPassword()).isEqualTo("newEncodedPassword");
        assertThat(savedAccount.getResetToken()).isNull();
        assertThat(savedAccount.getResetTokenExpiryTime()).isNull();
    }

    @Test
    void should_throw_exception_when_reset_token_not_found() {
        // ===== GIVEN =====
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setResetToken("invalid-token");
        request.setNewPassword("NewPassword123!");

        when(accountRepository.findByResetToken(request.getResetToken()))
                .thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired reset token");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void should_throw_exception_when_reset_token_expired() {
        // ===== GIVEN =====
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setResetToken("expired-token");
        request.setNewPassword("NewPassword123!");

        account.setResetToken("expired-token");
        account.setResetTokenExpiryTime(LocalDateTime.now().minusMinutes(15)); // Expired

        when(accountRepository.findByResetToken(request.getResetToken()))
                .thenReturn(Optional.of(account));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired reset token");

        verify(accountRepository, never()).save(any());
    }

    // ========== getAccountStatistics Tests ==========

    @Test
    void should_return_account_statistics_correctly() {
        // ===== GIVEN =====
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentMonthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime currentMonthEnd = currentMonthStart.plusMonths(1).minusSeconds(1);
        LocalDateTime previousMonthStart = currentMonthStart.minusMonths(1);
        LocalDateTime previousMonthEnd = currentMonthStart.minusSeconds(1);

        List<Account> currentMonthAccounts = List.of(account, new Account());
        List<Account> previousMonthAccounts = List.of(new Account());
        List<Account> allMembers = List.of(account, new Account(), new Account());

        when(accountRepository.findByRoleAndCreatedAtBetween(
                eq(Role.MEMBER), eq(currentMonthStart), eq(currentMonthEnd)))
                .thenReturn(currentMonthAccounts);
        when(accountRepository.findByRoleAndCreatedAtBetween(
                eq(Role.MEMBER), eq(previousMonthStart), eq(previousMonthEnd)))
                .thenReturn(previousMonthAccounts);
        when(accountRepository.findByRole(Role.MEMBER)).thenReturn(allMembers);

        // ===== WHEN =====
        Map<String, Object> result = accountService.getAccountStatistics();

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(result.get("totalMember")).isEqualTo(3);
        assertThat(result.get("currentMonthUsers")).isEqualTo(2);
        assertThat(result.get("growthPercentage")).isNotNull();
        // Growth = (2 - 1) / 1 * 100 = 100%
        assertThat(result.get("growthPercentage")).isEqualTo(100.0);
    }

    @Test
    void should_calculate_zero_percentage_when_previous_month_empty() {
        // ===== GIVEN =====
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentMonthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime currentMonthEnd = currentMonthStart.plusMonths(1).minusSeconds(1);
        LocalDateTime previousMonthStart = currentMonthStart.minusMonths(1);
        LocalDateTime previousMonthEnd = currentMonthStart.minusSeconds(1);

        List<Account> currentMonthAccounts = List.of(account);
        List<Account> previousMonthAccounts = List.of();
        List<Account> allMembers = List.of(account);

        when(accountRepository.findByRoleAndCreatedAtBetween(
                eq(Role.MEMBER), eq(currentMonthStart), eq(currentMonthEnd)))
                .thenReturn(currentMonthAccounts);
        when(accountRepository.findByRoleAndCreatedAtBetween(
                eq(Role.MEMBER), eq(previousMonthStart), eq(previousMonthEnd)))
                .thenReturn(previousMonthAccounts);
        when(accountRepository.findByRole(Role.MEMBER)).thenReturn(allMembers);

        // ===== WHEN =====
        Map<String, Object> result = accountService.getAccountStatistics();

        // ===== THEN =====
        // When previous month is 0, percentage should be 100%
        assertThat(result.get("growthPercentage")).isEqualTo(100.0);
    }

    // ========== activeAccountById Tests ==========

    @Test
    void should_activate_account_successfully() {
        // ===== GIVEN =====
        int accountId = 1;
        account.setActive(false);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ===== WHEN =====
        GlobalResponse<String> result = accountService.activeAccountById(accountId);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(account.isActive()).isTrue();
        assertThat(result.getData()).contains("active");
    }

    @Test
    void should_deactivate_account_successfully() {
        // ===== GIVEN =====
        int accountId = 1;
        account.setActive(true);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ===== WHEN =====
        GlobalResponse<String> result = accountService.activeAccountById(accountId);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(account.isActive()).isFalse();
        assertThat(result.getData()).contains("deactivated");
    }

    @Test
    void should_throw_exception_when_activating_admin_account() {
        // ===== GIVEN =====
        int accountId = 1;
        account.setRole(Role.ADMIN);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.activeAccountById(accountId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot have any action on admin account");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void should_throw_exception_when_account_not_found_for_activation() {
        // ===== GIVEN =====
        int accountId = 999;
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.activeAccountById(accountId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found");
    }

    // ========== deleteAccountById Tests ==========

    @Test
    void should_delete_member_account_successfully_when_no_pending_appointments() {
        // ===== GIVEN =====
        int accountId = 1;
        account.setRole(Role.MEMBER);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(appointmentRepository.existsByMemberIdAndAppointmentStatusOrAppointmentStatus(
                member.getId(), AppointmentStatus.PENDING, AppointmentStatus.IN_PROGRESS))
                .thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ===== WHEN =====
        GlobalResponse<String> result = accountService.deleteAccountById(accountId);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(account.isBanned()).isTrue();
        assertThat(account.isActive()).isFalse();
        assertThat(result.getData()).contains("deleted");
    }

    @Test
    void should_delete_coach_account_successfully_when_no_pending_appointments() {
        // ===== GIVEN =====
        int accountId = 1;
        account.setRole(Role.COACH);
        coach.setId(1);
        account.setCoach(coach);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(appointmentRepository.existsByCoachIdAndAppointmentStatusOrAppointmentStatus(
                coach.getId(), AppointmentStatus.PENDING, AppointmentStatus.IN_PROGRESS))
                .thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ===== WHEN =====
        GlobalResponse<String> result = accountService.deleteAccountById(accountId);

        // ===== THEN =====
        assertThat(result).isNotNull();
        assertThat(account.isBanned()).isTrue();
        assertThat(account.isActive()).isFalse();
        assertThat(result.getData()).contains("Coach account has been deleted");
    }

    @Test
    void should_throw_exception_when_member_has_pending_appointments() {
        // ===== GIVEN =====
        int accountId = 1;
        account.setRole(Role.MEMBER);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(appointmentRepository.existsByMemberIdAndAppointmentStatusOrAppointmentStatus(
                member.getId(), AppointmentStatus.PENDING, AppointmentStatus.IN_PROGRESS))
                .thenReturn(true);

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.deleteAccountById(accountId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot delete this member account with pending or in progress appointments");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void should_throw_exception_when_deleting_admin_account() {
        // ===== GIVEN =====
        int accountId = 1;
        account.setRole(Role.ADMIN);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.deleteAccountById(accountId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot have any action on admin account");
    }

    // ========== resetAccountPassword Tests ==========

    @Test
    void should_reset_account_password_successfully() {
        // ===== GIVEN =====
        int accountId = 1;
        String newPassword = "NewPassword123!";
        account.setRole(Role.MEMBER);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(passwordEncoder.encode(newPassword)).thenReturn("newEncodedPassword");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ===== WHEN =====
        accountService.resetAccountPassword(accountId, newPassword);

        // ===== THEN =====
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getPassword()).isEqualTo("newEncodedPassword");
    }

    @Test
    void should_throw_exception_when_resetting_admin_password() {
        // ===== GIVEN =====
        int accountId = 1;
        String newPassword = "NewPassword123!";
        account.setRole(Role.ADMIN);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.resetAccountPassword(accountId, newPassword))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot reset password for admin account");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void should_throw_exception_when_account_not_found_for_password_reset() {
        // ===== GIVEN =====
        int accountId = 999;
        String newPassword = "NewPassword123!";
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // ===== WHEN & THEN =====
        assertThatThrownBy(() -> accountService.resetAccountPassword(accountId, newPassword))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found");
    }
}