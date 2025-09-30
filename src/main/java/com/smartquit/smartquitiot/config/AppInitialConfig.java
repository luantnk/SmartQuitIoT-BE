package com.smartquit.smartquitiot.config;

import com.smartquit.smartquitiot.entity.Account;
import com.smartquit.smartquitiot.enums.AccountType;
import com.smartquit.smartquitiot.enums.Role;
import com.smartquit.smartquitiot.repository.AccountRepository;
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

    @Bean
    ApplicationRunner applicationRunner(){
        return args -> {
            if(accountRepository.findByRole(Role.ADMIN).isEmpty()){
                Account  account = new Account();
                account.setUsername("admin");
                account.setPassword(passwordEncoder.encode("admin"));
                account.setRole(Role.ADMIN);
                account.setAccountType(AccountType.SYSTEM);
                accountRepository.save(account);
                log.info("Admin account has been created");
            }
        };
    }

}
