package com.smartquit.smartquitiot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartquit.smartquitiot.dto.request.AddAchievementRequest;
import com.smartquit.smartquitiot.dto.response.AchievementDTO;
import com.smartquit.smartquitiot.dto.response.ConditionAchievementDTO;
import com.smartquit.smartquitiot.dto.response.TopMemberAchievementDTO;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.Achievement;
import com.smartquit.smartquitiot.entity.MemberAchievement;
import com.smartquit.smartquitiot.entity.Metric;
import com.smartquit.smartquitiot.repository.AchievementRepository;
import com.smartquit.smartquitiot.repository.MemberAchievementRepository;
import com.smartquit.smartquitiot.repository.MetricRepository;
import com.smartquit.smartquitiot.service.AccountService;
import com.smartquit.smartquitiot.service.MemberAchievementService;
import com.smartquit.smartquitiot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberAchievementServiceImpl implements MemberAchievementService {

    private final AccountService accountService;
    private final MemberAchievementRepository memberAchievementRepository;
    private final AchievementRepository achievementRepository;
    private final MetricRepository metricRepository;
    private final NotificationService notificationService;
    @Override
    @Transactional
    public Optional<Achievement> addMemberAchievement(AddAchievementRequest request) {
        log.info("Adding achievement for member " + request.getField());
        if (request.getField() == null) return Optional.empty();
        Account account = accountService.getAuthenticatedAccount();

        Metric metric = metricRepository.findByMemberId(account.getMember().getId()).orElse(null);

        log.info("metric " + metric.getId());
        int currentValue = readMetricValue(metric, request.getField());
        if (currentValue < 0) return Optional.empty();

        //get list achievement of users
        List<MemberAchievement>
                memberAchievements  = memberAchievementRepository.getMemberAchievementsByMember_Id(account.getMember().getId());

        Set<Integer> owned = memberAchievements.stream()
                .map(ma -> ma.getAchievement().getId())
                .collect(Collectors.toSet());

        // find all candidate achievements with condition.field == passed field
        List<Achievement> all = achievementRepository.findAll();

        List<AbstractMap.SimpleEntry<Achievement, ConditionAchievementDTO>> eligible =  all.stream()
                .map(a -> new AbstractMap.SimpleEntry<>(a, parseConditionDTO(a.getCondition())))
                .filter(e -> e.getValue() != null && request.getField().equals(e.getValue().getField()))
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

        if(highest != null){
            log.info("highest name: " + highest.getName());
            notificationService.saveAndSendAchievementNoti(account, highest);
        }


        return Optional.ofNullable(highest);
        // viết hàm check điều kiện nhận object, vì object có thể là streaks, steps,
        // money_saved,post_count, comment_count, total mission completed, completed_all_mission_in_day nằm trong bảng metric,
        // hãy check xem no1 là cái nào. xog sau đó get nó ra và so sánh với condition theo format {"field":"streaks","operator":">=","value":1}
        // sau đó nếu pass thì check tiếp xem họ đã sở hữu nó chưa, nếu chưa thì add họ vào và return trả ra achievement đó

    }

    @Override
    public List<AchievementDTO> getAllMyAchievements() {
        Account account = accountService.getAuthenticatedAccount();

        List<MemberAchievement> memberAchievements = memberAchievementRepository
               .getMemberAchievementsByMember_IdOrderByAchievedAt(account.getMember().getId());

        List<Achievement> allAchievements = achievementRepository.findAll();

        Map<Integer, MemberAchievement> ownedById = new HashMap<>();
        for (MemberAchievement ma : memberAchievements) {
            ownedById.put(ma.getAchievement().getId(), ma);
        }

        List<AchievementDTO> result = new ArrayList<>(allAchievements.size());
        for (Achievement a : allAchievements) {
            AchievementDTO dto = new AchievementDTO();
            dto.setId(a.getId());
            dto.setName(a.getName());
            dto.setDescription(a.getDescription());
            dto.setIcon(a.getIcon());
            dto.setType(a.getType().name());

            MemberAchievement hit = ownedById.get(a.getId());
            if (hit != null) {
                dto.setUnlocked(true);
                dto.setAchievedAt(hit.getAchievedAt());
            } else {
                dto.setUnlocked(false);
                dto.setAchievedAt(null);
            }
            result.add(dto);
        }
        result.sort(Comparator.comparing(AchievementDTO::isUnlocked).reversed()
                .thenComparing(AchievementDTO::getAchievedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        return result;
    }

    @Override
    public List<TopMemberAchievementDTO> getTop10MembersWithAchievements() {
        List<Object[]> top = memberAchievementRepository.findTop10MembersWithMostAchievements();

        List<TopMemberAchievementDTO> result = new ArrayList<>();
        for (Object[] row : top) {
            int memberId = ((Number) row[0]).intValue();
            int total = ((Number) row[1]).intValue();

            List<MemberAchievement> memberAchievements = memberAchievementRepository
                    .getMemberAchievementsByMember_IdOrderByAchievedAt(memberId);


            List<AchievementDTO> achievements = memberAchievements.stream().map(ma -> {
                Achievement a = ma.getAchievement();
                AchievementDTO dto = new AchievementDTO();
                dto.setId(a.getId());
                dto.setName(a.getName());
                dto.setDescription(a.getDescription());
                dto.setIcon(a.getIcon());
                dto.setType(a.getType().name());
             //   dto.setCondition(a.getCondition());
                dto.setAchievedAt(ma.getAchievedAt());
                return dto;
            }).toList();

            TopMemberAchievementDTO dto = new TopMemberAchievementDTO();
            dto.setMemberId(memberId);
            dto.setMemberName(memberAchievements.get(0).getMember().getFirstName() + " " + memberAchievements.get(0).getMember().getLastName());
            dto.setTotalAchievements(total);
            dto.setAchievements(achievements);
            dto.setAvatar_url(memberAchievements.get(0).getMember().getAvatarUrl());

            result.add(dto);
        }
        return result;
    }

    @Override
    public List<AchievementDTO> getMyAchievementsAtHome() {

        Account account = accountService.getAuthenticatedAccount();

        List<MemberAchievement> memberAchievements = memberAchievementRepository
                .getMemberAchievementsByMember_IdOrderByAchievedAt(account.getMember().getId());


        List<Achievement> allAchievements = achievementRepository.findAll();

        Map<Integer, MemberAchievement> ownedById = new HashMap<>();
        for (MemberAchievement ma : memberAchievements) {
            ownedById.put(ma.getAchievement().getId(), ma);
        }

        List<AchievementDTO> result = new ArrayList<>();
        for (Achievement a : allAchievements) {
            AchievementDTO dto = new AchievementDTO();
            dto.setId(a.getId());
            dto.setName(a.getName());
            dto.setDescription(a.getDescription());
            dto.setIcon(a.getIcon());
            dto.setType(a.getType().name());

            MemberAchievement hit = ownedById.get(a.getId());
            if (hit != null) {
                dto.setUnlocked(true);
                dto.setAchievedAt(hit.getAchievedAt());
            } else {
                dto.setUnlocked(false);
                dto.setAchievedAt(null);
            }
            result.add(dto);
        }
        result.sort(Comparator.comparing(AchievementDTO::isUnlocked).reversed()
                .thenComparing(AchievementDTO::getAchievedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        return result.stream()
                .limit(4)
                .collect(Collectors.toList());
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
