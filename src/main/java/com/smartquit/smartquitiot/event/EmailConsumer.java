package com.smartquit.smartquitiot.event;

import com.smartquit.smartquitiot.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = "${rabbitmq.queue.email.name}")
    public void handleEmailEvent(EmailMessageDTO message) {
        log.info("Received email event. Template: [{}], To: [{}]", message.getTemplateName(), message.getTo());

        try {
            Map<String, Object> props = message.getProps();

            switch (message.getTemplateName()) {
                case "otp":
                    String username = (String) props.get("username");
                    String otp = (String) props.get("otp");
                    emailService.sendHtmlOtpEmail(message.getTo(), message.getSubject(), username, otp);
                    break;

                case "payment-success":
                    String successName = (String) props.get("name");
                    String successPackage = (String) props.get("packageName");
                    long amount = ((Number) props.get("amount")).longValue();
                    long successOrderCode = ((Number) props.get("orderCode")).longValue();
                    emailService.sendPaymentSuccessEmail(message.getTo(), successName, successPackage, amount, successOrderCode);
                    break;

                case "payment-cancel":
                    String cancelName = (String) props.get("name");
                    String cancelPackage = (String) props.get("packageName");
                    long cancelOrderCode = ((Number) props.get("orderCode")).longValue();
                    emailService.sendPaymentCancelEmail(message.getTo(), cancelName, cancelPackage, cancelOrderCode);
                    break;

                default:
                    log.warn("Unknown email template received: {}", message.getTemplateName());
                    break;
            }
            log.info("Email processed successfully for: {}", message.getTo());

        } catch (Exception e) {
            log.error("Error processing email event for [{}]: {}", message.getTo(), e.getMessage(), e);
        }
    }
}