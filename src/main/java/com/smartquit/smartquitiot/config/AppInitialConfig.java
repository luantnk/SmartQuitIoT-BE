package com.smartquit.smartquitiot.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.*;
import com.smartquit.smartquitiot.repository.*;
import com.smartquit.smartquitiot.service.PhaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AppInitialConfig {

    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final SystemPhaseConditionRepository systemPhaseConditionRepository;
    private final ObjectMapper mapper = new ObjectMapper();
    private final MembershipPackageRepository membershipPackageRepository;
    private final MemberRepository memberRepository;
    private final MissionTypeRepository missionTypeRepository;
    private final InterestCategoryRepository interestCategoryRepository;
    private final PhaseService phaseService;



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
            initMembershipPackages();
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
                {"All Interests", "Universal category suitable for any user regardless of their interests missions here apply to everyone."},
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
      {"field": "progress", "operator": ">=", "value": 80}
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
               "value": 8
             },
             {
               "field": "avg_cigarettes",
               "operator": "<=",
               "formula": {
                 "base": "fm_cigarettes_total",
                 "operator": "*",
                 "percent": 0.8
               }
             }
           ]
         },
         { "field": "progress", "operator": ">=", "value": 80 }
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
               "value": 7
             },
             {
               "field": "avg_cigarettes",
               "operator": "<=",
               "formula": {
                     "base": "fm_cigarettes_total",
                 "operator": "*",
                 "percent": 0.7
               }
             }
           ]
         },
         { "field": "progress", "operator": ">=", "value": 80 }
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
              "value": 5
            },
            {
              "field": "avg_cigarettes",
              "operator": "<=",
              "formula": {
                "base": "fm_cigarettes_total",
                "operator": "*",
                "percent": 0.6
              }
            }
          ]
        },
        { "field": "progress", "operator": ">=", "value": 80 }
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
               "value": 3
             },
             {
               "field": "avg_cigarettes",
               "operator": "<=",
               "formula": {
                 "base": "fm_cigarettes_total",
                 "operator": "*",
                 "percent": 0.5
               }
             }
           ]
         },
         { "field": "progress", "operator": ">=", "value": 80 }
       ]
     }
    """
    };

    private void initMembershipPackages(){
        if(membershipPackageRepository.count() == 0){
            //generate free trial package
            MembershipPackage freeTrialPackage = new MembershipPackage();
            freeTrialPackage.setName("Free Trial");
            freeTrialPackage.setDescription("Free Trial Package");
            freeTrialPackage.setPrice(0L);
            freeTrialPackage.setType(MembershipPackageType.TRIAL);
            freeTrialPackage.setDuration(7);
            freeTrialPackage.setDurationUnit(DurationUnit.DAY);
            List<String> freeTrialFeatures = new ArrayList<>();
            freeTrialFeatures.add("Smart Quit Plan, Missions");
            freeTrialFeatures.add("Metrics Tracking");
            freeTrialPackage.setFeatures(freeTrialFeatures);
            membershipPackageRepository.save(freeTrialPackage);
            //generate standard package
            MembershipPackage standardPackage = new MembershipPackage();
            standardPackage.setName("Standard");
            standardPackage.setDescription("Standard Package");
            standardPackage.setPrice(79000L);
            standardPackage.setType(MembershipPackageType.STANDARD);
            standardPackage.setDuration(1);
            standardPackage.setDurationUnit(DurationUnit.MONTH);
            List<String> standardFeatures = new ArrayList<>();
            standardFeatures.add("Smart Quit Plan, Missions");
            standardFeatures.add("Metrics Tracking");
            standardPackage.setFeatures(standardFeatures);
            membershipPackageRepository.save(standardPackage);
            //generate premium package
            MembershipPackage premiumPackage = new MembershipPackage();
            premiumPackage.setName("Premium");
            premiumPackage.setDescription("Premium Package");
            premiumPackage.setPrice(100000L);
            premiumPackage.setType(MembershipPackageType.PREMIUM);
            premiumPackage.setDuration(1);
            premiumPackage.setDurationUnit(DurationUnit.MONTH);
            List<String> premiumFeatures = new ArrayList<>();
            premiumFeatures.add("Smart Quit Plan, Missions");
            premiumFeatures.add("Metrics Tracking");
            premiumFeatures.add("Guides by Coach");
            premiumFeatures.add("AI Personalize Chat");
            premiumPackage.setFeatures(premiumFeatures);
            membershipPackageRepository.save(premiumPackage);

            log.info("Initialized Membership Package");
        }

    }
}
