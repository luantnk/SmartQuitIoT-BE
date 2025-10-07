package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.CoachAccountRequest;
import com.smartquit.smartquitiot.dto.request.MemberAccountRequest;
import com.smartquit.smartquitiot.dto.response.CoachDTO;
import com.smartquit.smartquitiot.dto.response.MemberDTO;

public interface AccountService {

    MemberDTO registerMember(MemberAccountRequest request);

    CoachDTO registerCoach(CoachAccountRequest request);
}
