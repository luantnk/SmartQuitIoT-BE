package com.smartquit.smartquitiot.toolcalling;

import com.smartquit.smartquitiot.dto.response.DiaryRecordDTO;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
import com.smartquit.smartquitiot.dto.response.MissionTodayResponse;
import com.smartquit.smartquitiot.dto.response.QuitPlanResponse;
import com.smartquit.smartquitiot.entity.DiaryRecord;
import com.smartquit.smartquitiot.entity.HealthRecovery;
import com.smartquit.smartquitiot.entity.Metric;
import com.smartquit.smartquitiot.entity.QuitPlan;
import com.smartquit.smartquitiot.mapper.DiaryRecordMapper;
import com.smartquit.smartquitiot.mapper.QuitPlanMapper;
import com.smartquit.smartquitiot.repository.DiaryRecordRepository;
import com.smartquit.smartquitiot.repository.HealthRecoveryRepository;
import com.smartquit.smartquitiot.repository.MetricRepository;
import com.smartquit.smartquitiot.repository.QuitPlanRepository;
import com.smartquit.smartquitiot.service.MemberService;
import com.smartquit.smartquitiot.service.PhaseDetailMissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatbotTools {

    private final MemberService memberService;
    private final QuitPlanRepository quitPlanRepository;
    private final MetricRepository metricRepository;
    private final QuitPlanMapper quitPlanMapper;
    private final DiaryRecordMapper diaryRecordMapper;
    private final DiaryRecordRepository diaryRecordRepository;
    private final HealthRecoveryRepository healthRecoveryRepository;
    private final PhaseDetailMissionService phaseDetailMissionService;


    public static String CHATBOT_PROMPT = """
            You are "SmartQuitIoT Assistant," an AI companion chatbot for a smoking cessation app.
            Your role is to be a compassionate, understanding, and unconditionally supportive partner.
            Your primary goal is to motivate, educate, and provide actionable strategies to help users overcome cravings and stick to their quit goals.
            
            ### CORE RULES (MUST-FOLLOW):
            
            1.  **EMPATHY & NON-JUDGEMENT (The #1 Rule):**
                * You MUST NOT judge, criticize, blame, or make the user feel guilty, especially if they report a lapse or relapse (smoking again).
                * Always use positive, understanding, and validating language. Instead of "You failed," say, "That's okay, a single slip doesn't erase all your progress. Let's figure out what happened and get back on track."
                * Acknowledge the difficulty of quitting ("I understand this is incredibly challenging").
            
            2.  **TONE OF VOICE:**
                * Supportive, warm, and encouraging.
                * Clear, concise, and easy to understand.
                * Action-oriented. Prefer short replies that suggest a next step.
            
            3.  **ROLE BOUNDARIES (What you are NOT):**
                * **You are NOT a doctor or medical professional.**
                * Do not diagnose conditions, prescribe medication, or give specific medical advice (e.g., "You should use nicotine patches").
                * If the user asks for medical advice, you MUST direct them to "consult a doctor or healthcare professional."
            
            4.  **SAFETY & CRISIS HANDLING (CRITICAL):**
                * If the user's message clearly indicates self-harm, severe depression, or a mental health crisis, you MUST STOP providing quitting advice.
                * Immediately provide contact information for crisis hotlines (e.g., "I'm concerned about what you're saying. Please reach out to a crisis hotline or a mental health professional right away. You can call [Insert relevant crisis line number for the user's region].").
            
            5.  **DATA USAGE (Personalization):**
                * You must use the information provided in the [USER CONTEXT] section to personalize your responses.
                * Always reinforce positive milestones (days quit, money saved) to build confidence.
                * Use their stated "Triggers" and "Motivations" to give highly relevant advice.
            """;

    @Tool(name = "getMemberInformation",
            description = "Retrieve the member's information.")
    public MemberDTO getMemberInformation(
            @ToolParam(description = "The unique identifier of the member") Integer memberId
    ){
        return memberService.getMemberById(memberId);
    }

    @Tool(name = "getQuitPlanByMemberId",
            description = "Retrieve the quit plan information by member ID.")
    public QuitPlanResponse getQuitPlanByMemberId(
            @ToolParam(description = "The unique identifier of the member") Integer memberId){
        QuitPlan quitPlan = quitPlanRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId);
        if(quitPlan == null) return null;
        return quitPlanMapper.toQuitPlanResponse(quitPlan);
    }

    @Tool(name = "getMetricsByMemberId",
            description = "Retrieve the metrics information by member ID.")
    public Metric getMetricsByMemberId(
            @ToolParam(description = "The unique identifier of the member") Integer memberId){
        return metricRepository.findByMemberId(memberId).orElse(null);
    }

    @Tool(name = "getLatestDiaryRecordByMemberId",
            description = "Retrieve the latest diary record by member ID.")
    public DiaryRecordDTO getLatestDiaryRecordByMemberId(
            @ToolParam(description = "The unique identifier of the member") Integer memberId){
        Optional<DiaryRecord> previousDayRecord = diaryRecordRepository.findTopByMemberIdOrderByDateDesc(memberId);
        return previousDayRecord.map(diaryRecordMapper::toDiaryRecordDTO).orElse(null);
    }

    @Tool(name = "getHealthRecoveriesByMemberId",
            description = "Retrieve the health recovery records by member ID.")
    public List<HealthRecovery> getHealthRecoveriesByMemberId(
            @ToolParam(description = "The unique identifier of the member") Integer memberId) {
        return healthRecoveryRepository.findByMemberId(memberId);
    }

    @Tool(name = "getMissionsTodayByMemberId",
            description = "Retrieve today's missions for the member by their ID.")
    public MissionTodayResponse getMissionsTodayByMemberId(
            @ToolParam(description = "The unique identifier of the member") Integer memberId) {
        return phaseDetailMissionService.getListMissionTodayByMemberId(memberId);
    }
}
