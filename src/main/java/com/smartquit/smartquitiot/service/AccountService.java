package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.*;
import com.smartquit.smartquitiot.dto.response.CoachDTO;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.dto.response.VerifyOtpResponse;
import com.smartquit.smartquitiot.entity.Account;

public interface AccountService {

    MemberDTO registerMember(MemberAccountRequest request);

    CoachDTO registerCoach(CoachAccountRequest request);

    Account getAuthenticatedAccount();

    void updatePassword(ChangePasswordRequest request);

    void forgotPassword(String email);
    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);
    void resetPassword(ResetPasswordRequest request);
}
