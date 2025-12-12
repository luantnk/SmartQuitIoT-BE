package com.smartquit.smartquitiot.service;

import com.smartquit.smartquitiot.dto.request.MemberReminderSettingsRequest;
import com.smartquit.smartquitiot.dto.request.MemberUpdateRequest;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.dto.response.MemberListItemDTO;
import com.smartquit.smartquitiot.entity.Member;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MemberService {

    Member getAuthenticatedMember();
    MemberDTO getAuthenticatedMemberProfile();
    MemberDTO getMemberById(int id);
    MemberDTO updateProfile(MemberUpdateRequest request);
    Page<MemberDTO> getMembers(int page, int size, String search, Boolean isActive);
    List<MemberListItemDTO> getListMembers();
    MemberDTO updateReminderSettings(MemberReminderSettingsRequest req);
}
