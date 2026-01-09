package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.util.agora.RtcTokenBuilder2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgoraServiceImplTest {

    @InjectMocks
    private AgoraServiceImpl agoraService;

    private static final String APP_ID = "test-app-id";
    private static final String APP_CERT = "test-app-cert";

    @BeforeEach
    void setUp() {
        // inject @Value fields
        ReflectionTestUtils.setField(agoraService, "appId", APP_ID);
        ReflectionTestUtils.setField(agoraService, "appCertificate", APP_CERT);
    }

    // ================== SUCCESS CASES ==================

//    @Test
//    void should_use_minimum_ttl_when_ttl_is_zero_or_negative() {
//        try (MockedConstruction<RtcTokenBuilder2> mocked =
//                     mockConstruction(RtcTokenBuilder2.class,
//                             (mock, context) ->
//                                     when(mock.buildTokenWithUid(
//                                             anyString(),
//                                             anyString(),
//                                             anyString(),
//                                             anyInt(),
//                                             any(),
//                                             eq(30),
//                                             anyInt()
//                                     )).thenReturn("mock-token")
//                     )) {
//
//            String token1 = agoraService.generateRtcToken("channel", 100, 0);
//            String token2 = agoraService.generateRtcToken("channel", 100, -10);
//
//            assertThat(token1).isEqualTo("mock-token");
//            assertThat(token2).isEqualTo("mock-token");
//        }
//    }

    // ================== ERROR CASE ==================

    @Test
    void should_throw_exception_when_agora_sdk_fails() {
        try (MockedConstruction<RtcTokenBuilder2> mocked =
                     mockConstruction(RtcTokenBuilder2.class,
                             (mock, context) ->
                                     when(mock.buildTokenWithUid(
                                             anyString(),
                                             anyString(),
                                             anyString(),
                                             anyInt(),
                                             any(),
                                             anyInt(),
                                             anyInt()
                                     )).thenThrow(new RuntimeException("SDK error"))
                     )) {

            assertThatThrownBy(() ->
                    agoraService.generateRtcToken("channel", 100, 3600)
            )
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Lỗi khi tạo token Agora");
        }
    }
}
