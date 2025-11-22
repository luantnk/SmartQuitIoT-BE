package com.smartquit.smartquitiot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${allowed.url}")
    private String allowedOrigins;
    @Autowired
    private CustomJwtDecoder customJwtDecoder;
    private static final List<Map.Entry<String, HttpMethod>> SECURED_URLS = List.of(
            Map.entry("/accounts/coach/create", HttpMethod.POST),
            Map.entry("/accounts/p", HttpMethod.GET),
            Map.entry("/accounts/activate/{accountId}", HttpMethod.PUT),
            Map.entry("/accounts/ban/{accountId}", HttpMethod.PUT),
            Map.entry("/members/p", HttpMethod.GET),
            Map.entry("/members/manage", HttpMethod.GET),
            Map.entry("/members", HttpMethod.PUT),
            Map.entry("/coaches/p", HttpMethod.GET),
            Map.entry("/coaches/all", HttpMethod.GET),
            Map.entry("/coaches/{coachId}", HttpMethod.PUT),
            Map.entry("/membership-packages/create-payment-link", HttpMethod.POST),
            Map.entry("/membership-packages/process", HttpMethod.POST),
            Map.entry("/membership-subscriptions/current", HttpMethod.GET),
            Map.entry("/interest-category/all", HttpMethod.GET),
            Map.entry("/quit-plan/create-in-first-login", HttpMethod.POST),
            Map.entry("/quit-plan", HttpMethod.GET),
            Map.entry("/phase/home-page", HttpMethod.GET),
            Map.entry("/phase-detail-mission/mission-today", HttpMethod.GET),
            Map.entry("/phase-detail-mission/complete/home-page", HttpMethod.POST),
            Map.entry("/diary-records/log", HttpMethod.POST),
            Map.entry("/diary-records/history", HttpMethod.POST),
            Map.entry("/metrics/home-screen", HttpMethod.GET),
            Map.entry("/metrics/health-data", HttpMethod.GET),
            Map.entry("/missions", HttpMethod.GET),
            Map.entry("/news", HttpMethod.POST),
            Map.entry("/news/{id}", HttpMethod.DELETE),
            Map.entry("/news/{id}", HttpMethod.PUT),
            Map.entry("/achievement/add-member-achievement", HttpMethod.POST),
            Map.entry("/achievement/all-my-achievements", HttpMethod.GET),
            Map.entry("/achievement/top-leader-boards", HttpMethod.GET),
            Map.entry("/achievement/my-achievements-at-home", HttpMethod.GET),
            Map.entry("/quit-plan/time", HttpMethod.GET),
            Map.entry("/achievement/all", HttpMethod.GET),
            Map.entry("/quit-plan/time", HttpMethod.GET),
            Map.entry("/quit-plan/keep-plan", HttpMethod.POST),
            Map.entry("/quit-plan/create-new", HttpMethod.POST),
            Map.entry("/form-metric", HttpMethod.GET),
            Map.entry("/form-metric", HttpMethod.POST),
            Map.entry("/quit-plan/all-quit-plan", HttpMethod.GET),
            Map.entry("/quit-plan/specific/{id}", HttpMethod.GET),
            Map.entry("/phase/redo", HttpMethod.POST),
            Map.entry("/system-phase-condition", HttpMethod.GET),
            Map.entry("/system-phase-condition/{id}", HttpMethod.PUT),
            Map.entry("/system-phase-condition/test", HttpMethod.POST),
            Map.entry("/members/settings/reminder", HttpMethod.PUT),
            Map.entry("/achievement/{id}", HttpMethod.GET)
    );

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> {
                    SECURED_URLS.forEach(entry -> authorize.requestMatchers(entry.getValue(), entry.getKey()).authenticated());
                    authorize.anyRequest().permitAll();
                });
        http.oauth2ResourceServer(
                server -> server
                        .jwt(
                                jwtConfig -> jwtConfig.decoder(customJwtDecoder)
                                        .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                        );

        http.cors(cors -> cors.configurationSource(request -> {
            CorsConfiguration corsConfiguration = new CorsConfiguration();
            corsConfiguration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
            corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
            corsConfiguration.setAllowedHeaders(List.of("*"));
            return corsConfiguration;
        }));
//        http.cors(cors -> cors.configurationSource(request -> {
//            CorsConfiguration corsConfiguration = new CorsConfiguration();
//            // split & trim same biến allowedOrigins
//            List<String> patterns = Arrays.stream(allowedOrigins.split(","))
//                    .map(String::trim)
//                    .filter(s -> !s.isEmpty())
//                    .collect(Collectors.toList());
//            // if empty, set dev defaults or throw
//            if (patterns.isEmpty()) {
//                patterns = List.of("http://localhost:5173");
//            }
//            corsConfiguration.setAllowedOriginPatterns(patterns);
//            corsConfiguration.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
//            corsConfiguration.setAllowedHeaders(List.of("*"));
//            corsConfiguration.setAllowCredentials(true);
//            return corsConfiguration;
//        }));


        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}