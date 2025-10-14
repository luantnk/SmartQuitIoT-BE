package com.smartquit.smartquitiot.service;

public interface EmailService {
    void sendHtmlOtpEmail(String to, String subject, String username, String otp);
}
