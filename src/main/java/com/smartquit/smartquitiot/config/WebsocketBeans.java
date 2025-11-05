package com.smartquit.smartquitiot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@Configuration
public class WebsocketBeans {

    @Bean("jwtChannelInterceptor")
    public ChannelInterceptor jwtChannelInterceptor(JwtDecoder jwtDecoder) {
        return new WebSocketJwtAuthInterceptor(jwtDecoder);
    }
}
