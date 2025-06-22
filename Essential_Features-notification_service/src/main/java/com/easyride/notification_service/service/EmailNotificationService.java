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
        // Overloaded method for backward compatibility or simple notifications
        return sendEmail(recipient, "Notification", message);
    }

    /**
     * Sends an email with a specified subject and body.
     * @param recipient The recipient's email address.
     * @param subject The subject of the email.
     * @param message The body of the email.
     * @return true if the email was sent successfully, false otherwise.
     */
    public boolean sendEmail(String recipient, String subject, String message) {
        try {
            // Set mail server properties
            Properties properties = new Properties();
            properties.put("mail.smtp.host", smtpHost);
            properties.put("mail.smtp.port", smtpPort);
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");

            // Create an authenticator
            Authenticator authenticator = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };

            // Create a Session object
            Session session = Session.getInstance(properties, authenticator);

            // Create the email
            MimeMessage mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(username));
            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            mimeMessage.setSubject(subject);
            mimeMessage.setText(message);

            // Send the email
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