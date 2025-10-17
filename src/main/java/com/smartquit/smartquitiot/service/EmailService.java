package com.smartquit.smartquitiot.service;

public interface EmailService {
    void sendHtmlOtpEmail(String to, String subject, String username, String otp);
    void sendPaymentSuccessEmail(String to, String name, String packageName, long amount, long orderCode);
    void sendPaymentCancelEmail(String to, String name, String packageName, long orderCode);

}
