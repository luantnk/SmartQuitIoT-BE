package com.smartquit.smartquitiot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TopMemberAchievementDTO {
    private int memberId;
    private String memberName;
    private long totalAchievements;
    private List<AchievementDTO> achievements;
}
