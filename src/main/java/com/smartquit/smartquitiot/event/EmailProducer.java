package com.smartquit.smartquitiot.event;

import com.smartquit.smartquitiot.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.email.name}")
    private String emailExchange;

    @Value("${rabbitmq.routing.email.key}")
    private String emailRoutingKey;

    public void sendEmailEvent(EmailMessageDTO emailMessage) {
        log.info("Preparing to send email event to queue. Subject: [{}], Template: [{}]",
                emailMessage.getSubject(), emailMessage.getTemplateName());
        try {
            rabbitTemplate.convertAndSend(emailExchange, emailRoutingKey, emailMessage);
            log.info("Email event successfully published to exchange: [{}]", emailExchange);
        } catch (Exception e) {
            log.error("Failed to publish email event. Error: {}", e.getMessage());
        }
    }
}