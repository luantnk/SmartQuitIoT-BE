package com.smartquit.smartquitiot.service.impl;
import com.smartquit.smartquitiot.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
}