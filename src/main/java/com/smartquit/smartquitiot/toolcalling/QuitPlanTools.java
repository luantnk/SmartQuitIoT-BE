package com.smartquit.smartquitiot.toolcalling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuitPlanTools {
    public static final String SYSTEM_PROMPT = """
            You are a smoking cessation assistant.
            You must generate a personalized quit plan using exactly 5 sequential phases:
            1. Preparation
            2. Onset
            3. Peak Craving
            4. Subsiding
            5. Maintenance
            
            Use the tool `calculatePhaseDuration(ftnd, smokeAvgPerDay, yearsSmoking, age, gender)` 
            to determine the duration (in days) for each phase.
            
            Important structure and date rules:
            - The quit plan begins at `startDateOfQuitPlan`, which is the same as the user's StartDate.
            - The phases must be continuous, non-overlapping, and ordered.
            - Each phase’s startDate should immediately follow the previous phase’s endDate.
            - The `endDateOfQuitPlan` must be the end date of the final phase (Maintenance).
            - Always explain the reason for each phase duration briefly using user data.
            """;


    @Tool(name = "calculatePhaseDuration",
            description = "Compute how many days each quit plan phase should last based on user data.")
    public Map<String, Integer> calculatePhaseDuration(
            @ToolParam(description = "Fagerström nicotine dependence score") int FTND,
            @ToolParam(description = "Average number of cigarettes per day") int smokeAvgPerDay,
            @ToolParam(description = "Total years of smoking") int yearsSmoking,
            @ToolParam(description = "User age in years") int age,
            @ToolParam(description = "User gender, either MALE or FEMALE") String gender
    ) {
        Map<String, Integer> phases = new HashMap<>();
        //  Preparation
        int prep;
        if (FTND >= 7 || smokeAvgPerDay > 20) prep = 3;
        else if (FTND <= 3 && smokeAvgPerDay < 10) prep = 2;
        else prep = 2;
        phases.put("Preparation", prep);
        // Onset
        int onset;
        if (FTND >= 7 || smokeAvgPerDay > 15) onset = 7;
        else if (FTND <= 3) onset = 4;
        else onset = 5;
        if ("FEMALE".equalsIgnoreCase(gender)) onset += 1;
        if (age >= 40) onset += 1;
        phases.put("Onset", onset);

        // Peak Craving
        int peak;
        if (FTND >= 7 || yearsSmoking > 10) peak = 10;
        else if (FTND <= 3 && smokeAvgPerDay < 10) peak = 6;
        else peak = 8;
        if ("FEMALE".equalsIgnoreCase(gender)) peak += 2;
        phases.put("Peak Craving", peak);
        // Subsiding
        int sub;
        if (FTND >= 7) sub = 18;
        else if (FTND <= 3) sub = 13;
        else sub = 15;
        if (age >= 50) sub += 3;
        phases.put("Subsiding", sub);
        // Maintenance
        int main;
        if (yearsSmoking > 10 || age >= 50) main = 30;
        else if (yearsSmoking > 5) main = 25;
        else main = 20;
        if ("FEMALE".equalsIgnoreCase(gender)) main += 7;
        phases.put("Maintenance", main);
        return phases;
    }

}
