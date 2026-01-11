package com.smartquit.smartquitiot.event;

import com.smartquit.smartquitiot.config.RabbitMQConfig;
import com.smartquit.smartquitiot.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailConsumer {

    private final EmailService emailService;
    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void handleEmailEvent(EmailMessageDTO message) {
        if ("otp".equals(message.getTemplateName())) {
            String username = (String) message.getProps().get("username");
            String otp = (String) message.getProps().get("otp");
            emailService.sendHtmlOtpEmail(message.getTo(), message.getSubject(), username, otp);
        }
        else if ("payment-success".equals(message.getTemplateName())) {

        }
    }
}