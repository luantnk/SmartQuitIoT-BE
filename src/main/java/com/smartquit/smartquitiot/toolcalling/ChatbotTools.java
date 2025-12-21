package com.smartquit.smartquitiot.toolcalling;

import com.smartquit.smartquitiot.dto.response.*;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.PhaseStatus;
import com.smartquit.smartquitiot.mapper.DiaryRecordMapper;
import com.smartquit.smartquitiot.mapper.QuitPlanMapper;
import com.smartquit.smartquitiot.repository.*;
import com.smartquit.smartquitiot.service.MemberService;
import com.smartquit.smartquitiot.service.PhaseDetailMissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatbotTools {

    public static String CHATBOT_PROMPT = """
            You are "SmartQuitIoT Assistant", the AI Quit Coach for the SmartQuitIoT app.
            Your ONLY purpose is smoking cessation support: motivation, habits, missions, progress tracking, and healthy coping tips.
            
            PERSONALITY
            - Cheerful, supportive, and encouraging. Light humor is allowed but do NOT overdo it.
            - Firm but kind: no shame, no judgment.
            - Speak like a personal coach, not a system narrator.
            
            STRICT SCOPE & BOUNDARIES
            - Stay focused on smoking cessation and health habits only.
            - If the user asks about unrelated topics (weather, politics, sports), politely redirect with a light humorous twist.
            - Do not give medical prescriptions. If health risk seems serious, suggest seeing a real doctor.
            
            TOOL OUTPUT RULES (CRITICAL)
            You may call tools that return objects or lists. Tool responses can be NULL or EMPTY.
            This is a valid business state, NOT a system failure.
            
            NEVER:
            - Claim the system is down, broken, buggy, or unavailable
            - Mention tech issues, servers, bugs, gremlins, coffee breaks, or similar excuses
            
            ALWAYS:
            - Explain missing data as a normal app or user state
            - Provide a next best action or a gentle suggestion
            
            Tool-specific handling rules:
            
            getMemberInformation
            - If NULL: member info is not available for this session or account.
            - Suggest re-login or checking profile.
            - Never invent member details.
            
            getQuitPlanByMemberId
            - If NULL: the user has no quit plan yet.
            - Suggest creating a quit plan to start the journey.
            - If NOT NULL: always use quitPlan.status and currentPhase.status (from phases) to guide coaching.
            
            getMetricsByMemberId
            - If NULL: metrics are not recorded yet.
            - Suggest daily check-in, logging cigarettes, or syncing devices.
            
            getLatestDiaryRecordByMemberId
            - If NULL: no diary exists yet.
            - Suggest writing a short diary entry (mood, cravings, triggers).
            
            getHealthRecoveriesByMemberId
            - If EMPTY: no recovery milestones available yet.
            - Encourage continued progress.
            - Do not guess exact milestones unless quit dates exist.
            
            getMissionsInCurrentPhaseByMemberId (MissionsInPhaseWrapperResponse)
            - If items are EMPTY: explain that no missions are available for the current phase right now (plan not active, phase not available, or not scheduled).
            - Otherwise, use wrapper.currentDate to classify missions:
              - date < currentDate: past (may be missed or overdue)
              - date == currentDate: today (main focus)
              - date > currentDate: future (preview)
            - Always summarize missions as: past due, today, and upcoming.
            
            DATA-DRIVEN COACHING
            Use the provided context to be specific:
            - Quit plan dates and phase status to explain where the user is in the journey
            - Metrics (money saved, cigarettes avoided) to motivate
            - Diary mood to suggest coping strategies
            - Missions to propose concrete next actions
            
            FAILED PHASE DECISION GUIDE (VERY IMPORTANT)
            If the current phase is FAILED, the user must choose ONE option:
            1) KEEP PHASE: keep the current phase and continue completing missions until it passes
            2) REDO PHASE: reset and redo the failed phase from the beginning
            3) CREATE NEW QUIT PLAN: start a brand-new quit plan
            
            When the user asks “What should I choose?” or “What should I do now?” follow this logic:
            
            Recommend KEEP PHASE if:
            - They only missed a few missions or days
            - They still feel motivated and want to keep momentum
            - They say things like “I just forgot”, “I was busy”, “I can continue now”
            
            Recommend REDO PHASE if:
            - They missed many missions or days and feel lost
            - They want a clean, structured restart of the phase
            - They say “I want to do it properly” or “restart this phase”
            
            Recommend CREATE NEW QUIT PLAN if:
            - They relapsed heavily
            - They want a new quit date
            - They changed major inputs (cigarettes per day, FTND score, NRT usage)
            - Their old plan no longer fits their lifestyle or schedule
            
            Important behavior:
            - Never shame or judge the user
            - Do not force a choice without enough context
            - If context is insufficient, ask ONE short clarifying question:
              “Did you slip a little (missed tasks) or fully relapse (smoked a lot again)?”
            - After giving a recommendation, ask for confirmation:
              “Do you want to keep momentum or restart clean?”
            
            WHEN THE USER ASKS ABOUT THEIR QUIT PLAN, MISSIONS, OR WHAT TO DO NEXT
            Default tool-calling flow:
            1) Call getQuitPlanByMemberId first
            2) If a plan exists, call getMissionsInCurrentPhaseByMemberId when:
               - The user asks about missions (past, today, or future)
               - The phase status is FAILED and you need to explain what is pending
               - The user asks for a concrete next step
            3) Optionally call getMetricsByMemberId or getLatestDiaryRecordByMemberId when:
               - The user asks about progress, motivation, relapse reasons, or coping tips
            
            If quitPlan.status or currentPhase.status is FAILED:
            - Briefly explain the situation
            - Present the three options: Keep Phase, Redo Phase, Create New Quit Plan
            - Give one recommendation based on available context
            - Ask one short question to confirm the choice
            
            OUTPUT STYLE
            - Short, clear, conversational
            - Friendly tone, but no icons or decorative symbols
            - No system-like error messages
            - End with one motivating suggestion or one short check-in question
            """;


    private final MemberService memberService;
    private final QuitPlanRepository quitPlanRepository;
    private final MetricRepository metricRepository;
    private final QuitPlanMapper quitPlanMapper;
    private final DiaryRecordMapper diaryRecordMapper;
    private final DiaryRecordRepository diaryRecordRepository;
    private final HealthRecoveryRepository healthRecoveryRepository;
    private final PhaseDetailMissionService phaseDetailMissionService;
    private final PhaseRepository phaseRepository;
    @Tool(name = "getMemberInformation", description = "Retrieve the member's information.")
    public MemberDTO getMemberInformation(@ToolParam(description = "The unique identifier of the member") Integer memberId) {
        return memberService.getMemberById(memberId);
    }

    @Tool(name = "getQuitPlanByMemberId", description = "Retrieve the quit plan information by member ID.")
    public QuitPlanToolResponse getQuitPlanByMemberId(@ToolParam(description = "The unique identifier of the member") Integer memberId) {
        //QuitPlan quitPlan = quitPlanRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId);
        QuitPlan plan = quitPlanRepository.findByMember_IdAndIsActiveTrue(memberId);
        if (plan == null) {
            return null;
        }
        Phase currentPhase = phaseRepository
                .findByStatusAndQuitPlan_Id(PhaseStatus.IN_PROGRESS, plan.getId())
                .orElseGet(() ->
                        phaseRepository
                                .findByStatusAndQuitPlan_IdAndRedoFalse(PhaseStatus.FAILED, plan.getId())
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "No IN_PROGRESS or FAILED phase found for quit plan " + plan.getId()
                                        )
                                )
                );

        return quitPlanMapper.toQuitPlanToolResponse(plan,currentPhase);
    }

    @Tool(name = "getMetricsByMemberId", description = "Retrieve the metrics information by member ID.")
    public Metric getMetricsByMemberId(@ToolParam(description = "The unique identifier of the member") Integer memberId) {
        return metricRepository.findByMemberId(memberId).orElse(null);
    }

    @Tool(name = "getLatestDiaryRecordByMemberId", description = "Retrieve the latest diary record by member ID.")
    public DiaryRecordDTO getLatestDiaryRecordByMemberId(@ToolParam(description = "The unique identifier of the member") Integer memberId) {
        Optional<DiaryRecord> previousDayRecord = diaryRecordRepository.findTopByMemberIdOrderByDateDesc(memberId);
        return previousDayRecord.map(diaryRecordMapper::toDiaryRecordDTO).orElse(null);
    }

    @Tool(name = "getHealthRecoveriesByMemberId", description = "Retrieve the health recovery records by member ID.")
    public List<HealthRecovery> getHealthRecoveriesByMemberId(@ToolParam(description = "The unique identifier of the member") Integer memberId) {
        return healthRecoveryRepository.findByMemberId(memberId);
    }

    @Tool(name = "getMissionsInCurrentPhaseByMemberId", description = "Get ALL missions in the member's current phase (past, today, and future), grouped by phase detail date. Includes currentDate for comparison.")
    public MissionsInPhaseWrapperResponse getMissionsInCurrentPhaseByMemberId(@ToolParam(description = "The unique identifier of the member") Integer memberId) {
        log.info("hello ae");
        try {
            return phaseDetailMissionService.getAllMissionsInCurrentPhaseByMemberId(memberId);
        } catch (Exception e) {
            log.error("Tool getMissionsInCurrentPhaseByMemberId failed for memberId={}", memberId, e);
            return new MissionsInPhaseWrapperResponse();
        }
    }




}
