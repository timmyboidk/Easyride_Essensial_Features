package com.easyride.notification_service.service;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService implements NotificationService {

    @Value("${mail.smtp.host}")
    private String smtpHost;

    @Value("${mail.smtp.port}")
    private String smtpPort;

    @Value("${mail.username}")
    private String username;

    @Value("${mail.password}")
    private String password;

    @Override
    public boolean sendNotification(String recipient, String message) {
        return sendEmail(recipient, message);
    }

    /**
     * 发送邮件的实现方法
     */
    public boolean sendEmail(String recipient, String message) {
        try {
            // 设置邮件服务器属性
            Properties properties = new Properties();
            properties.put("mail.smtp.host", smtpHost);
            properties.put("mail.smtp.port", smtpPort);
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");

            // 创建认证对象
            Authenticator authenticator = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };

            // 创建 Session 对象
            Session session = Session.getInstance(properties, authenticator);

            // 创建邮件
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(username));
            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            mimeMessage.setSubject("Notification");
            mimeMessage.setText(message);

            // 发送邮件
            Transport.send(mimeMessage);
            System.out.println("Email sent successfully to: " + recipient);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Error while sending email: " + e.getMessage());
            return false;
        }
    }
}

