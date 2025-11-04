package com.smartquit.smartquitiot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ChannelInterceptor cho WebSocket STOMP:
 * - Đọc header "Authorization" khi CONNECT
 * - Decode token bằng JwtDecoder (CustomJwtDecoder bạn đã có)
 * - Thiết lập accessor.setUser(authentication) để STOMP session có principal
 * - Thiết lập SecurityContextHolder cho luồng xử lý message, và clear sau khi gửi xong
 *
 */
@Component("jwtChannelInterceptor")
public class WebSocketJwtAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketJwtAuthInterceptor.class);

    private final JwtDecoder jwtDecoder;

    public WebSocketJwtAuthInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        try {
            // Thực hiện khi client CONNECT (lần đầu) hoặc có header auth gửi trong message
            if (StompCommand.CONNECT.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand())) {
                List<String> authHeaders = accessor.getNativeHeader("Authorization");
                if (authHeaders != null && !authHeaders.isEmpty()) {
                    String raw = authHeaders.get(0);
                    String token = raw.startsWith("Bearer ") ? raw.substring(7) : raw;
                    if (token != null && !token.isBlank()) {
                        Jwt jwt = jwtDecoder.decode(token);

                        // lấy principal name: ưu tiên claim "accountId", fallback subject, fallback username claim
                        String principalName = null;
                        Object accIdObj = jwt.getClaims().get("accountId");
                        if (accIdObj != null) {
                            principalName = String.valueOf(accIdObj);
                        }
                        if (principalName == null || principalName.isEmpty()) {
                            principalName = jwt.getSubject();
                        }
                        if (principalName == null) {
                            Object username = jwt.getClaims().get("username");
                            principalName = username != null ? String.valueOf(username) : "anonymous";
                        }

                        // lấy roles/scope từ claim (project của bạn dùng "scope" claim string)
                        Collection<SimpleGrantedAuthority> authorities = parseAuthoritiesFromJwt(jwt);

                        Authentication auth = new UsernamePasswordAuthenticationToken(principalName, token, authorities);

                        // gán user cho STOMP session
                        accessor.setUser(auth);

                        // Đặt Authentication vào SecurityContext tạm thời để service có thể dùng SecurityContextHolder
                        SecurityContextHolder.getContext().setAuthentication(auth);

                        // log ngắn gọn
                        log.debug("WebSocket CONNECT/SEND authenticated principal={} authorities={}", principalName, authorities);
                    }
                }
            }
        } catch (Exception ex) {
            // nếu decode lỗi: không block toàn bộ kết nối; client sẽ gặp lỗi khi subscribe/send nếu cần auth
            log.warn("WebSocket JWT decode failed: {}", ex.getMessage());
        }
        return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        // clear SecurityContextHolder nếu principal ở accessor chính là authentication hiện tại
        try {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            Authentication principal = (Authentication) accessor.getUser();
            Authentication current = SecurityContextHolder.getContext().getAuthentication();
            if (principal != null && principal.equals(current)) {
                SecurityContextHolder.clearContext();
                log.debug("Cleared SecurityContext after STOMP message processing for principal={}", principal.getName());
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private Collection<SimpleGrantedAuthority> parseAuthoritiesFromJwt(Jwt jwt) {
        // Project đặt claim "scope" = ROLE_NAME trong token generation.
        try {
            Object scopeObj = jwt.getClaims().get("scope");
            List<String> roles = new ArrayList<>();
            if (scopeObj instanceof String s) {
                // có thể là "MEMBER" hoặc "ROLE_MEMBER"
                String str = s.trim();
                if (!str.isEmpty()) {
                    roles.addAll(Arrays.asList(str.split("[,\\s]+")));
                }
            } else if (scopeObj instanceof Collection<?> coll) {
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
