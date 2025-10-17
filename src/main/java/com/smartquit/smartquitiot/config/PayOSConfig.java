package com.smartquit.smartquitiot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;


@Configuration
public class PayOSConfig {

    @Value("${payos.clientId}")
    private String clientId;

    @Value("${payos.apiKey}")
    private String apiKey;

    @Value("${payos.checksumKey}")
    private String checksumKey;

    @Bean
    public PayOS payOS() {
        // SDK mới khởi tạo trực tiếp không cần ClientOptions
        return new PayOS(clientId, apiKey, checksumKey);
    }
}
