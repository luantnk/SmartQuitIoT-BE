package com.smartquit.smartquitiot.controller;

import com.smartquit.smartquitiot.dto.request.MessageCreateRequest;
import com.smartquit.smartquitiot.dto.request.WsMessageRequest;
import com.smartquit.smartquitiot.dto.response.MessageDTO;
import com.smartquit.smartquitiot.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * WS controller for /app/conversations/messages
 * - Primary flow: jwtChannelInterceptor should set Principal at CONNECT
 * - Fallback: try read Authorization from the frame's native headers and decode with JwtDecoder
 *
 * NOTE: fallback only helps debugging/dev. Production: rely on interceptor.
 */
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final JwtDecoder jwtDecoder; // use your CustomJwtDecoder bean

    @MessageMapping("/conversations/messages")
    public void onMessage(@Payload WsMessageRequest payload,
                          SimpMessageHeaderAccessor headerAccessor,
                          Principal principal) {
        Authentication previousAuth = SecurityContextHolder.getContext().getAuthentication();
        boolean setTempAuth = false;

        try {
            // If session already has a principal from interceptor, use it.
            if (principal == null || SecurityContextHolder.getContext().getAuthentication() == null) {
                // Attempt robust fallback: read Authorization from native headers in many forms
                String rawAuth = extractAuthFromAccessor(headerAccessor);
                if (rawAuth == null) {
                    log.warn("STOMP message received without principal - no Authorization header found in frame (session={})", headerAccessor.getSessionId());
                    // try to notify FE: if FE provided username/accountId header, use that
                    sendErrorToUserByHeader(headerAccessor, "Not authenticated. Please include Authorization Bearer token in STOMP CONNECT headers.");
                    return;
                }

                String token = rawAuth.startsWith("Bearer ") ? rawAuth.substring(7).trim() : rawAuth.trim();
                if (token.isEmpty()) {
                    log.warn("STOMP message received without principal - empty token after strip (session={})", headerAccessor.getSessionId());
                    sendErrorToUserByHeader(headerAccessor, "Missing token");
                    return;
                }

                try {
                    Jwt jwt = jwtDecoder.decode(token);

                    // Build principalName: prefer accountId, fallback subject, fallback username claim
                    String principalName = null;
                    Object accIdObj = jwt.getClaims().get("accountId");
                    if (accIdObj != null) principalName = String.valueOf(accIdObj);
                    if (principalName == null || principalName.isEmpty()) principalName = jwt.getSubject();
                    if (principalName == null || principalName.isEmpty()) {
                        Object username = jwt.getClaims().get("username");
                        principalName = username != null ? String.valueOf(username) : "unknown";
                    }

                    Collection<SimpleGrantedAuthority> authorities = parseAuthoritiesFromJwt(jwt);
                    Authentication tempAuth = new UsernamePasswordAuthenticationToken(principalName, token, authorities);
                    SecurityContextHolder.getContext().setAuthentication(tempAuth);
                    setTempAuth = true;
                    log.debug("[FallbackAuth] set temporary Authentication principal={} authorities={} session={}", principalName, authorities, headerAccessor.getSessionId());
                } catch (Exception ex) {
                    log.warn("[FallbackAuth] failed to decode token from headers: {}", ex.getMessage());
                    sendErrorToUserByHeader(headerAccessor, "Invalid or expired token: " + ex.getMessage());
                    return;
                }
            }

            // Use authenticated principal (either original or temporary)
            Authentication effective = (Authentication) SecurityContextHolder.getContext().getAuthentication();
            String principalName = (effective != null) ? effective.getName() : (principal != null ? principal.getName() : "unknown");

            log.info("[WS] recv from principal={} conv={} clientMessageId={}", principalName, payload.getConversationId(), payload.getClientMessageId());

            // Map payload -> service DTO
            MessageCreateRequest req = new MessageCreateRequest();
            req.setConversationId(payload.getConversationId());
            req.setTargetUserId(payload.getTargetUserId());
            req.setMessageType(payload.getMessageType());
            req.setContent(payload.getContent());
            req.setAttachments(payload.getAttachments());
            req.setClientMessageId(payload.getClientMessageId());

            // Persist + broadcast
            MessageDTO saved = messageService.sendMessage(req);
            log.debug("WS: message persisted id={} conv={}", saved != null ? saved.getId() : null, saved != null ? saved.getConversationId() : null);

            // Temporary debug echo for FE correlation (remove if messageService already broadcasts)
            if (saved != null) {
                try {
                    saved.setClientMessageId(req.getClientMessageId());
                    String topic = "/topic/conversations/" + saved.getConversationId();
                    log.info("[WS DEBUG] echoing to topic {} clientMessageId={}", topic, req.getClientMessageId());
                    messagingTemplate.convertAndSend(topic, saved);
                } catch (Exception e) {
                    log.error("[WS DEBUG] echo failed", e);
                }
            }
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid WS payload: {}", ex.getMessage());
            sendErrorToUser(SecurityContextHolder.getContext().getAuthentication(), "Bad request: " + ex.getMessage());
        } catch (SecurityException ex) {
            log.warn("Security error on WS message: {}", ex.getMessage());
            sendErrorToUser(SecurityContextHolder.getContext().getAuthentication(), "Forbidden: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Error handling WS message", ex);
            sendErrorToUser(SecurityContextHolder.getContext().getAuthentication(), "Server error");
        } finally {
            // restore previous security context if we changed it
            try {
                if (setTempAuth) {
                    SecurityContextHolder.getContext().setAuthentication(previousAuth);
                    log.debug("[FallbackAuth] restored previous SecurityContext (session={})", headerAccessor.getSessionId());
                }
            } catch (Exception e) {
                log.debug("[FallbackAuth] failed to restore SecurityContext: {}", e.getMessage());
            }
        }
    }

    @MessageExceptionHandler
    public void handleException(Exception ex, Principal principal) {
        log.error("STOMP handler exception: {}", ex.getMessage(), ex);
        sendErrorToUser(principal, "Server error: " + ex.getMessage());
    }

    // ---- helpers ----

    private String extractAuthFromAccessor(SimpMessageHeaderAccessor accessor) {
        try {
            // 1) prefer getFirstNativeHeader (supported in many versions)
            String first = null;
            try {
                first = accessor.getFirstNativeHeader("Authorization");
            } catch (NoSuchMethodError | AbstractMethodError ignore) {
                // some exotic runtime; fallback below
            }
            if (first == null) first = accessor.getFirstNativeHeader("authorization");
            if (first == null) first = accessor.getFirstNativeHeader("Auth");
            if (first != null) return first;

            // 2) try getNativeHeader(key) which returns List<String>
            try {
                List<String> alt = accessor.getNativeHeader("Authorization");
                if (alt == null) alt = accessor.getNativeHeader("authorization");
                if (alt == null) alt = accessor.getNativeHeader("Auth");
                if (alt != null && !alt.isEmpty()) return alt.get(0);
            } catch (NoSuchMethodError | AbstractMethodError ignore) {
            }

            // 3) Fallback: inspect message headers map for "nativeHeaders" entry
            Object nh = accessor.getMessageHeaders().get("nativeHeaders");
            if (nh instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) nh;
                for (String key : List.of("Authorization", "authorization", "Auth", "auth", "token", "accessToken")) {
                    Object v = map.get(key);
                    if (v instanceof List<?> lst && !lst.isEmpty()) return String.valueOf(lst.get(0));
                    if (v != null) return String.valueOf(v);
                }
            }

            // 4) NEW: check session attributes (set by interceptor on CONNECT)
            try {
                Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
                if (sessionAttrs != null && !sessionAttrs.isEmpty()) {
                    for (String key : List.of("Authorization", "authorization", "auth", "token", "accessToken", "principalName")) {
                        Object v = sessionAttrs.get(key);
                        if (v instanceof String s && !s.isEmpty()) return s;
                    }
                }
            } catch (Throwable ignored) {
            }

            // 5) Last resort: check common alt header keys directly in message headers
            for (String key : List.of("Authorization", "authorization", "Auth", "auth", "token", "accessToken")) {
                Object candidate = accessor.getMessageHeaders().get(key);
                if (candidate instanceof String s && !s.isEmpty()) return s;
            }
        } catch (Exception e) {
            log.debug("extractAuthFromAccessor error: {}", e.getMessage());
        }
        return null;
    }

    private void sendErrorToUser(Principal principal, String message) {
        try {
            if (principal != null) {
                messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", message);
            }
        } catch (Exception e) {
            log.warn("Failed to send WS error to user: {}", e.getMessage());
        }
    }

    private void sendErrorToUserByHeader(SimpMessageHeaderAccessor headerAccessor, String message) {
        try {
            List<String> possible = null;
            try { possible = headerAccessor.getNativeHeader("username"); } catch (Throwable ignored) {}
            if (possible == null || possible.isEmpty()) {
                try { possible = headerAccessor.getNativeHeader("accountId"); } catch (Throwable ignored) {}
            }
            if (possible != null && !possible.isEmpty()) {
                String user = possible.get(0);
                messagingTemplate.convertAndSendToUser(user, "/queue/errors", message);
                return;
            }

            // fallback: check session attributes
            try {
                Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
                if (sessionAttrs != null) {
                    Object pn = sessionAttrs.get("principalName");
                    if (pn instanceof String pns && !pns.isEmpty()) {
                        messagingTemplate.convertAndSendToUser(String.valueOf(pn), "/queue/errors", message);
                        return;
                    }
                }
            } catch (Throwable ignored) {}

        } catch (Exception ignored) {}

        Authentication auth = (Authentication) SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) sendErrorToUser(auth, message);
        else log.warn("Cannot send WS error to user - unknown principal/session");
    }

    private Collection<SimpleGrantedAuthority> parseAuthoritiesFromJwt(Jwt jwt) {
        try {
            Object scopeObj = jwt.getClaims().get("scope");
            List<String> roles = new ArrayList<>();
            if (scopeObj instanceof String) {
                String s = (String) scopeObj;
                if (!s.trim().isEmpty()) roles.addAll(Arrays.asList(s.trim().split("[,\\s]+")));
            } else if (scopeObj instanceof Collection<?>) {
                for (Object o : (Collection<?>) scopeObj) if (o != null) roles.add(String.valueOf(o));
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
