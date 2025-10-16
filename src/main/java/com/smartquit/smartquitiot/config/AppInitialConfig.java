package com.smartquit.smartquitiot.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartquit.smartquitiot.entity.*;
import com.smartquit.smartquitiot.enums.*;
import com.smartquit.smartquitiot.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AppInitialConfig {

    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final SystemPhaseConditionRepository systemPhaseConditionRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    private final ObjectMapper mapper = new ObjectMapper();
    private final MembershipPackageRepository membershipPackageRepository;

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
            if (systemPhaseConditionRepository.count() == 0) {
                initSystemPhaseCondition();
            }
            initMembershipPackages();
        };
    }

//    @Bean
//    ApplicationRunner initDemoPostsWithComments() {
//        return args -> {
//            if (postRepository.count() == 0) {
//                // Lấy admin account để gắn post và comment
//                Account admin = accountRepository.findByUsername("admin")
//                        .orElseThrow(() -> new RuntimeException("Admin account not found"));
//
//                for (int i = 1; i <= 3; i++) {
//                    Post post = new Post();
//                    post.setTitle("Demo Post " + i);
//                    post.setDescription("This is a description for demo post " + i);
//                    post.setContent("Full content of demo post " + i + ". Lorem ipsum dolor sit amet...");
//                    post.setThumbnail("https://via.placeholder.com/150");
//                    post.setAccount(admin);
//
//                    // thêm media demo
//                    PostMedia media1 = new PostMedia();
//                    media1.setMediaUrl("https://via.placeholder.com/600");
//                    media1.setMediaType(MediaType.IMAGE);
//                    media1.setPost(post);
//
//                    PostMedia media2 = new PostMedia();
//                    media2.setMediaUrl("https://via.placeholder.com/800");
//                    media2.setMediaType(MediaType.IMAGE);
//                    media2.setPost(post);
//
//                    post.setMedia(List.of(media1, media2));
//
//                    postRepository.save(post);
//
//                    // Thêm comment cha
//                    Comment comment1 = new Comment();
//                    comment1.setContent("This is a comment 1 for post " + i);
//                    comment1.setAccount(admin);
//                    comment1.setPost(post);
//
//                    // Comment con (reply)
//                    Comment reply1 = new Comment();
//                    reply1.setContent("This is a reply to comment 1");
//                    reply1.setAccount(admin);
//                    reply1.setParent(comment1);
//                    reply1.setPost(post);
//
//                    // Comment cha 2
//                    Comment comment2 = new Comment();
//                    comment2.setContent("This is a comment 2 for post " + i);
//                    comment2.setAccount(admin);
//                    comment2.setPost(post);
//
//                    comment1.setReplies(List.of(reply1));
//                    post.setComments(List.of(comment1, comment2));
//
//                    // Save comments manually
//                    commentRepository.save(comment1);
//                    commentRepository.save(reply1);
//                    commentRepository.save(comment2);
//
//                    postRepository.save(post);
//                }
//
//                log.info("Demo posts with comments have been created!");
//            }
//        };
//    }


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
