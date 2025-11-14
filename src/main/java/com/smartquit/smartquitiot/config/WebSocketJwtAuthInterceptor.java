package com.smartquit.smartquitiot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.*;
import java.util.stream.Collectors;

public class WebSocketJwtAuthInterceptor implements ChannelInterceptor {
    private static final Logger log = LoggerFactory.getLogger(WebSocketJwtAuthInterceptor.class);
    private final JwtDecoder jwtDecoder;

    public WebSocketJwtAuthInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        try {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                // read possible header names
                String raw = accessor.getFirstNativeHeader("Authorization");
                if (raw == null) raw = accessor.getFirstNativeHeader("authorization");
                if (raw == null) raw = accessor.getFirstNativeHeader("Auth");

                if (raw == null) {
                    // sometimes nativeHeaders nested under "nativeHeaders"
                    Object nh = accessor.getMessageHeaders().get("nativeHeaders");
                    if (nh instanceof Map<?, ?>) {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> map = (Map<String, List<String>>) nh;
                        for (String k : List.of("Authorization","authorization","Auth","auth","token","accessToken")) {
                            List<String> vals = map.get(k);
                            if (vals != null && !vals.isEmpty()) { raw = vals.get(0); break; }
                        }
                    }
                }

                if (raw == null) {
                    log.debug("WS CONNECT without Authorization header (anonymous session).");
                    return message; // allow anonymous if you want — or return null to reject
                }

                String token = raw.startsWith("Bearer ") ? raw.substring(7).trim() : raw.trim();
                if (token.isEmpty()) {
                    log.debug("Empty token in CONNECT");
                    return null; // reject CONNECT frame
                }

                // decode token -> if expired/invalid, reject CONNECT quickly
                Jwt jwt;
                try {
                    jwt = jwtDecoder.decode(token);
                } catch (Exception ex) {
                    log.warn("WS CONNECT token invalid/expired: {}", ex.getMessage());
                    // reject CONNECT so FE sees failure (recommended during dev)
                    return null;
                }

                // Build principalName: prefer accountId claim (numeric) else sub
                String principalName = null;
                Object accClaim = jwt.getClaims().get("accountId");
                if (accClaim == null) accClaim = jwt.getClaims().get("account_id");
                if (accClaim != null) principalName = String.valueOf(accClaim);
                if (principalName == null || principalName.isBlank()) principalName = jwt.getSubject();

                // parse scope/roles -> SimpleGrantedAuthority
                Collection<SimpleGrantedAuthority> authorities = parseAuthorities(jwt);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principalName, token, authorities);
                accessor.setUser(auth);
                log.debug("WS CONNECT: principal={} set on session", principalName);

                // *** NEW: persist token/principal into session attributes so later SEND frames can read them ***
                try {
                    Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
                    if (sessionAttrs != null) {
                        sessionAttrs.put("Authorization", token);
                        sessionAttrs.put("principalName", principalName);
                        // also useful: raw jwt claims if you need quick access
                        sessionAttrs.put("jwt_claims_sub", jwt.getSubject());
                    }
                } catch (Exception e) {
                    log.debug("Failed to write session attributes for WS CONNECT: {}", e.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("WS auth interceptor error (CONNECT): {}", ex.getMessage());
            // failing here: either allow anonymous or reject; safer to reject
            return null;
        }
        return message;
    }

    private Collection<SimpleGrantedAuthority> parseAuthorities(Jwt jwt) {
        try {
            Object scope = jwt.getClaims().get("scope");
            List<String> roles = new ArrayList<>();
            if (scope instanceof String s && !s.isBlank()) {
                roles.addAll(Arrays.asList(s.split("[,\\s]+")));
            } else if (scope instanceof Collection<?> coll) {
                for (Object o : coll) if (o != null) roles.add(String.valueOf(o));
            }
            return roles.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(r -> !r.isEmpty())
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }
}
