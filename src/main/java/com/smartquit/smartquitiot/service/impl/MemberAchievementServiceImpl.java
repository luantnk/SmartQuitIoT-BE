package com.smartquit.smartquitiot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartquit.smartquitiot.dto.response.ConditionAchievementDTO;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Achievement;
import com.smartquit.smartquitiot.entity.MemberAchievement;
import com.smartquit.smartquitiot.entity.Metric;
import com.smartquit.smartquitiot.repository.AchievementRepository;
import com.smartquit.smartquitiot.repository.MemberAchievementRepository;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.service.MemberAchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberAchievementServiceImpl implements MemberAchievementService {

    private final AccountService accountService;
    private final MemberAchievementRepository memberAchievementRepository;
    private final AchievementRepository achievementRepository;

    @Override
    @Transactional
    public Optional<Achievement> addAchievement(String field) {
        if (field == null) return Optional.empty();
        Account account = accountService.getAuthenticatedAccount();
        Metric metric = account.getMember().getMetric();
        int currentValue = readMetricValue(metric, field);
        if (currentValue < 0) return Optional.empty();

        //get list achievement of users
        List<MemberAchievement>
                memberAchievements  = memberAchievementRepository.getAchievements(account.getMember().getId());

        Set<Integer> owned = memberAchievements.stream()
                .map(ma -> ma.getAchievement().getId())
                .collect(Collectors.toSet());

        // find all candidate achievements with condition.field == passed field
        List<Achievement> all = achievementRepository.findAll();

        List<AbstractMap.SimpleEntry<Achievement, ConditionAchievementDTO>> eligible =  all.stream()
                .map(a -> new AbstractMap.SimpleEntry<>(a, parseConditionDTO(a.getCondition())))
                .filter(e -> e.getValue() != null && field.equals(e.getValue().getField()))
                .filter(e -> compare(currentValue, e.getValue().getOperator(), e.getValue().getValue()))
                .filter(e -> !owned.contains(e.getKey().getId()))
                .collect(Collectors.toList());

        if (eligible.isEmpty()) return Optional.empty();
        //lưu nếu trường hợp 1 lúc đạt được 2 mốc
        List<MemberAchievement> toSave = eligible.stream()
                .map(e -> {
                    MemberAchievement ma = new MemberAchievement();
                    ma.setMember(account.getMember());
                    ma.setAchievement(e.getKey());
                    return ma;
                })
                .collect(Collectors.toList());
        memberAchievementRepository.saveAll(toSave);
        // trả về mốc cao nhất
        Achievement highest = eligible.stream()
                .max(Comparator.comparingInt(e -> e.getValue().getValue())) // so sánh theo threshold
                .map(Map.Entry::getKey)
                .orElse(null);
        return Optional.ofNullable(highest);
        // viết hàm check điều kiện nhận object, vì object có thể là streaks, steps,
        // money_saved,post_count, comment_count, total mission completed, completed_all_mission_in_day nằm trong bảng metric,
        // hãy check xem no1 là cái nào. xog sau đó get nó ra và so sánh với condition theo format {"field":"streaks","operator":">=","value":1}
        // sau đó nếu pass thì check tiếp xem họ đã sở hữu nó chưa, nếu chưa thì add họ vào và return trả ra achievement đó

    }


    private int readMetricValue(Metric metric, String field) {
        switch (field) {
            case "streaks": return metric.getStreaks();
            case "steps": return metric.getSteps();
            case "money_saved":
                return metric.getMoneySaved() != null
                        ? metric.getMoneySaved().setScale(0, RoundingMode.FLOOR).intValue()
                        : 0;
            case "post_count": return metric.getPost_count();
            case "comment_count": return metric.getComment_count();
            case "total_mission_completed": return metric.getTotal_mission_completed();
            case "completed_all_mission_in_day": return metric.getCompleted_all_mission_in_day();
            default: return -1;
        }
    }
    private ConditionAchievementDTO parseConditionDTO(JsonNode node) {
        if (node == null || node.isNull()) return null;

        JsonNode conditionNode = node.isArray() ? node.get(0) : node;

        String field = conditionNode.path("field").asText(null);
        String op    = conditionNode.path("operator").asText(null);
        int val      = conditionNode.path("value").asInt(Integer.MIN_VALUE);

        if (field == null || op == null || val == Integer.MIN_VALUE) return null;

        return new ConditionAchievementDTO(field, op, val);
    }


    private boolean compare(int actual, String operator, int target) {
        switch (operator) {
            case ">=": return actual >= target;
            case ">" : return actual >  target;
            case "<=": return actual <= target;
            case "<" : return actual <  target;
            case "==":
            case "=" : return actual == target;
            case "!=": return actual != target;
            default  : return false;
        }
    }


}
