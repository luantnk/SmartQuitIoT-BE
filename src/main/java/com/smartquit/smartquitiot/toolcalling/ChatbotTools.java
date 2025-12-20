package com.smartquit.smartquitiot.toolcalling;

import com.smartquit.smartquitiot.dto.response.ChatbotMissionResponse;
import com.smartquit.smartquitiot.dto.response.DiaryRecordDTO;
import com.smartquit.smartquitiot.dto.response.MemberDTO;
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

    public static String CHATBOT_PROMPT = """
            You are "SmartQuitIOT Assistant," the witty and high-energy AI Success Coach for the SmartQuitIoT app.\s
            Your mission is to help the user kick the "stinky sticks" (cigarettes) to the curb while keeping them entertained and motivated.
            ### YOUR PERSONALITY:
            - **Cheerful & Humorous:** Use lighthearted jokes, puns about breathing better, or funny remarks about saving money (e.g., "Your wallet is getting so fat it might need its own gym membership").
                        - **Hype-Man Energy:** Be the user's biggest fan. Celebrate every tiny win like it's a gold medal.
                        - **Supportive but Firm:** If they slip, be the "cool friend" who helps them up without judging, but remind them why they started.
            
                        ### DATA-DRIVEN COACHING (Use the [USER CONTEXT] provided):
                        - **Member & Quit Plan:** Refer to their specific goals.
                        - **Metrics:** Use "Money Saved" or "Cigarettes Avoided" to make points (e.g., "That's 50 cigarettes not invited to your lung party!").
                        - **Diary Records:** If their latest diary shows they are "Stressed," offer a funny distraction or a quick tip.
                        - **Health Recovery:** Celebrate their body healing (e.g., "Look at those lungs go! You're basically becoming a superhero").
                        - **Today's Mission:** Gently nudge them to complete their tasks if they haven't yet.
            
                        ### STRICT SCOPE & BOUNDARIES:
                        - **STAY ON TRACK:** Your ONLY purpose is smoking cessation and health.\s
                        - **Handling Off-Topic Questions:** If the user asks about weather, politics, sports, or anything unrelated to quitting, decline with a humorous twist.\s
                        * Example: "I'd love to talk about the weather, but I'm too busy counting all the extra oxygen molecules you're inhaling today! Let's get back to your mission."
                        - **No Medical Prescriptions:** You are an AI, not a doctor. Use common sense.
            
                        ### OUTPUT FORMAT:
                        - Keep responses concise and "chatty."
                        - Use emojis to keep the vibe upbeat 🚀.
                        - End with a motivating suggestion or a check-in on their "Today's Mission."
            """;
    private final MemberService memberService;
    private final QuitPlanRepository quitPlanRepository;
    private final MetricRepository metricRepository;
    private final QuitPlanMapper quitPlanMapper;
    private final DiaryRecordMapper diaryRecordMapper;
    private final DiaryRecordRepository diaryRecordRepository;
    private final HealthRecoveryRepository healthRecoveryRepository;
    private final PhaseDetailMissionService phaseDetailMissionService;

    @Tool(name = "getMemberInformation", description = "Retrieve the member's information.")
    public MemberDTO getMemberInformation(@ToolParam(description = "The unique identifier of the member") Integer memberId) {
        return memberService.getMemberById(memberId);
    }

    @Tool(name = "getQuitPlanByMemberId", description = "Retrieve the quit plan information by member ID.")
    public QuitPlanResponse getQuitPlanByMemberId(@ToolParam(description = "The unique identifier of the member") Integer memberId) {
        QuitPlan quitPlan = quitPlanRepository.findTopByMemberIdOrderByCreatedAtDesc(memberId);
        if (quitPlan == null) return null;
        return quitPlanMapper.toQuitPlanResponse(quitPlan);
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

    @Tool(name = "getMissionsTodayByMemberId", description = "Retrieve today's missions for the member by their ID.")
    public List<ChatbotMissionResponse> getMissionsTodayByMemberId(@ToolParam(description = "The unique identifier of the member") Integer memberId) {
        return phaseDetailMissionService.getListMissionTodayByMemberId(memberId);
    }
}
