package com.smartquit.smartquitiot.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.entity.SystemPhaseCondition;
import com.smartquit.smartquitiot.enums.AccountType;
import com.smartquit.smartquitiot.enums.Role;
import com.smartquit.smartquitiot.repository.AccountRepository;
import com.smartquit.smartquitiot.repository.SystemPhaseConditionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AppInitialConfig {

    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final SystemPhaseConditionRepository systemPhaseConditionRepository;
    private final ObjectMapper mapper = new ObjectMapper();

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
        };
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
