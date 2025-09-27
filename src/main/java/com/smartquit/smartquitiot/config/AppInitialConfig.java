package com.smartquit.smartquitiot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AppInitialConfig {

    private final PasswordEncoder passwordEncoder;

}
