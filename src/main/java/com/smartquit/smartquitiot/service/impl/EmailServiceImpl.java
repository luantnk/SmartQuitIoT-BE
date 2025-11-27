package com.smartquit.smartquitiot.service.impl;

import com.smartquit.smartquitiot.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Override
    public void sendHtmlOtpEmail(String to, String subject, String username, String otp) {
        try {
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("otp", otp);
            String htmlContent = templateEngine.process("otp-email", context);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            byte[] logoBytes = Files.readAllBytes(Path.of("src/main/resources/images/logo.png"));
            InputStreamSource logoSource = new ByteArrayResource(logoBytes);
            helper.addInline("logoImage", logoSource, "image/png");
            mailSender.send(mimeMessage);
        } catch (MessagingException | IOException e) {
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    @Override
    public void sendPaymentSuccessEmail(String to, String name, String packageName, long amount, long orderCode) {
        try {
            NumberFormat format = NumberFormat.getInstance(Locale.US);
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("packageName", packageName);
            context.setVariable("amount", format.format(amount));
            context.setVariable("orderCode", orderCode);
            String htmlContent = templateEngine.process("payment-success-email", context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("SmartQuit - Payment Confirmation for Order #" + orderCode);
            helper.setText(htmlContent, true);
            helper.addInline("logoImage", new ClassPathResource("images/logo.png"));
            mailSender.send(message);
            log.info("✅ Payment success email sent to {}", to);
        } catch (MessagingException e) {
            log.error("❌ Failed to send payment success email: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendPaymentCancelEmail(String to, String name, String packageName, long orderCode) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("packageName", packageName);
            context.setVariable("orderCode", orderCode);

            String htmlContent = templateEngine.process("payment-cancel-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("SmartQuit - Payment Cancelled for Order #" + orderCode);
            helper.setText(htmlContent, true);
            helper.addInline("logoImage", new ClassPathResource("images/logo.png"));

            mailSender.send(message);
            log.info("⚠️ Payment cancellation email sent to {}", to);
        } catch (MessagingException e) {
            log.error("❌ Failed to send payment cancellation email to {}: {}", to, e.getMessage(), e);
        }
    }


}