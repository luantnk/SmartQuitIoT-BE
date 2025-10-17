package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.entity.Member;

public interface MemberService {

    Member getAuthenticatedMember();
    MemberDTO getAuthenticatedMemberProfile();
}
