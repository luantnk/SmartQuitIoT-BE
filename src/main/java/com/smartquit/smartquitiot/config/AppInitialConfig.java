package com.smartquit.smartquitiot.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.AccountType;
import com.smartquit.smartquitiot.enums.Gender;
import com.smartquit.smartquitiot.enums.Role;
import com.smartquit.smartquitiot.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AppInitialConfig {

    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final SystemPhaseConditionRepository systemPhaseConditionRepository;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MemberRepository memberRepository;
    private final MissionTypeRepository missionTypeRepository;
    private final InterestCategoryRepository interestCategoryRepository;




    @Bean
    ApplicationRunner applicationRunner() {
        return args -> {
            if (accountRepository.findByRole(Role.ADMIN).isEmpty()) {
                Account account = new Account();
                account.setUsername("admin");
                account.setEmail("admin@smartquit.io.vn");
                account.setPassword(passwordEncoder.encode("admin"));
                account.setRole(Role.ADMIN);
                account.setFirstLogin(false);
                account.setAccountType(AccountType.SYSTEM);
                accountRepository.save(account);
                log.info("Admin account has been created");
            }
            if (accountRepository.findByRole(Role.MEMBER).isEmpty()) {
                Account account2 = new Account();
                account2.setUsername("member1");
                account2.setEmail("member1@smartquit.io.vn");
                account2.setPassword(passwordEncoder.encode("member1"));
                account2.setRole(Role.MEMBER);
                account2.setFirstLogin(true);
                account2.setAccountType(AccountType.SYSTEM);
                Member member = new Member();
                member.setFirstName("mem");
                member.setLastName("mem");
                member.setAvatarUrl("https://cdn.smartquit.io.vn/avatars/member1.png");
                member.setGender(Gender.MALE);
                member.setDob(LocalDate.of(2001, 5, 12));
                account2.setMember(member);
                member.setAccount(account2);
                account2.setMember(member);
                memberRepository.save(member);
                log.info("Member1 account has been created");
            }

            log.info("member1 account has been created");
            if (systemPhaseConditionRepository.count() == 0) {
                initSystemPhaseCondition();
            }
            if (missionTypeRepository.count() == 0) {
                initMissionTypes();
            }
            if (interestCategoryRepository.count() == 0) {
                initInterestCategories();
            }

        };
    }
    private void initInterestCategories() {
        String[][] interestCategories = {
                {"All Interests", "Universal category suitable for any user regardless of their interests — missions here apply to everyone."},
                {"Sports and Exercise", "Activities that improve fitness, strength, and overall physical health."},
                {"Art and Creativity", "Expressing creativity through drawing, painting, crafting, or other artistic activities."},
                {"Cooking and Food", "Exploring new recipes, enjoying healthy meals, and discovering culinary experiences."},
                {"Reading, Learning and Writing", "Gaining knowledge, practicing writing skills, and enjoying books or educational content."},
                {"Music and Entertainment", "Listening to music, playing instruments, or engaging with entertainment to relax and recharge."},
                {"Nature and Outdoor Activities", "Connecting with nature through outdoor walks, gardening, hiking, or spending time in fresh air."}
        };

        for (String[] data : interestCategories) {
            InterestCategory category = new InterestCategory();
            category.setName(data[0]);
            category.setDescription(data[1]);
            interestCategoryRepository.save(category);
        //    log.info("Interest Category '{}' has been initialized.", data[0]);
        }

     log.info("All interest categories initialized successfully!");
    }


    private void initMissionTypes() {
        String[][] missionTypeData = {
                {"Health Improvement", "Missions focused on building healthier habits, strengthening the body, and reducing the damage caused by smoking."},
                {"Coping", "Missions that provide strategies to manage cravings, stress, and emotional triggers without cigarettes."},
                {"Support", "Missions that encourage connecting with friends, family, or the community for encouragement and accountability."},
                {"Planning", "Missions that help users create, adjust, and follow a structured quit plan for long-term success."},
                {"Awareness", "Missions that increase knowledge about smoking risks and the benefits of quitting, empowering users with better decisions."},
                {"NRT", "Missions that provide Nicotine Replacement Therapy."}
        };

        for (String[] data : missionTypeData) {
            MissionType type = new MissionType();
            type.setName(data[0]);
            type.setDescription(data[1]);
            missionTypeRepository.save(type);
          //  log.info("MissionType '{}' has been initialized.", data[0]);
        }
    }
    private void initSystemPhaseCondition() {
        for (int i = 0; i < jsonPhaseConditionStrings.length; i++) {
            try {
                JsonNode jsonNode = mapper.readTree(jsonPhaseConditionStrings[i]);
                SystemPhaseCondition systemPhaseCondition = new SystemPhaseCondition();
                systemPhaseCondition.setCondition(jsonNode);
                systemPhaseCondition.setName(nameSystemPhaseCondition[i]);
                systemPhaseConditionRepository.save(systemPhaseCondition);
                log.info("System Phase Condition: " + systemPhaseCondition.getName() + " saved!");
            } catch (Exception e) {
                throw new RuntimeException("error in initSystemPhaseCondition", e);
            }
        }
    }

    private final String[] nameSystemPhaseCondition = {
            "Preparation",
            "Onset",
            "Peak Craving",
            "Subsiding",
            "Maintenance"
    };
    private final String[] jsonPhaseConditionStrings = {
            // preparation
            """
  {
    "rules": [
      {"field": "mission_avg", "operator": ">=", "value": 80}
    ],
    "logic": "AND"
  }
  """,

            // onset
            """
    {
       "logic": "AND",
       "rules": [
         {
           "logic": "OR",
           "rules": [
              {
               "field": "craving_level_avg",
               "operator": "<=",
               "formula": {
                 "base": "craving_level_avg",
                 "operator": "*",
                 "percent": 0.3
               }
             },
             {
               "field": "cigarettes_total",
               "operator": "<=",
               "formula": {
                 "base": "cigarettes_total",
                 "operator": "*",
                 "percent": 0.5
               }
             }
           ]
         },
         { "field": "mission_avg", "operator": ">=", "value": 80 }
       ]
     }
    """,

            // peak craving
            """
    {
       "logic": "AND",
       "rules": [
         {
           "logic": "OR",
           "rules": [
              {
               "field": "craving_level_avg",
               "operator": "<=",
               "formula": {
                 "base": "craving_level_avg",
                 "operator": "*",
                 "percent": 0.3
               }
             },
             {
               "field": "cigarettes_total",
               "operator": "<=",
               "formula": {
                 "base": "cigarettes_total",
                 "operator": "*",
                 "percent": 0.8
               }
             }
           ]
         },
         { "field": "mission_avg", "operator": ">=", "value": 80 }
       ]
     }
    """,

            // subsiding
            """
    {
      "logic": "AND",
      "rules": [
        {
          "logic": "OR",
          "rules": [
             {
              "field": "craving_level_avg",
              "operator": "<=",
              "value": 2.5
            },
            {
              "field": "cigarettes_total",
              "operator": "<=",
              "formula": {
                "base": "cigarettes_total",
                "operator": "*",
                "percent": 1
              }
            }
          ]
        },
        { "field": "mission_avg", "operator": ">=", "value": 80 }
      ]
    }
    """,
            // Maintenence
            """
    {
       "logic": "AND",
       "rules": [
         {
           "logic": "OR",
           "rules": [
              {
               "field": "craving_level_avg",
               "operator": "<=",
               "value": 1.5
             },
             {
               "field": "cigarettes_total",
               "operator": "<=",
               "formula": {
                 "base": "cigarettes_total",
                 "operator": "*",
                 "percent": 0.5
               }
             }
           ]
         },
         { "field": "mission_avg", "operator": ">=", "value": 80 }
       ]
     }
    """
    };
}
