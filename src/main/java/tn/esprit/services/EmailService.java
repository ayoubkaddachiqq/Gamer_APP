package tn.esprit.services;

import java.io.InputStream;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailService {

    private final String smtpHost;
    private final String smtpPort;
    private final String fromEmail;
    private final String fromPassword;
    private final boolean enabled;

    public EmailService() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("email.properties")) {
            if (is != null) props.load(is);
        } catch (Exception e) {
            System.err.println("[EmailService] No email.properties found, using defaults");
        }
        smtpHost = props.getProperty("smtp.host", "smtp.gmail.com");
        smtpPort = props.getProperty("smtp.port", "587");
        fromEmail = props.getProperty("smtp.from", "");
        fromPassword = props.getProperty("smtp.password", "");
        enabled = !fromEmail.isEmpty() && !fromPassword.isEmpty();
    }

    public boolean sendResetEmail(String to, String token) {
        System.out.println("[EmailService] Token for " + to + ": " + token);
        if (!enabled) return false;
        String subject = "Team Hub - Password Reset";
        String body = "<h2>Password Reset</h2>"
                    + "<p>Use this code to reset your password:</p>"
                    + "<h3 style='color:#00E5FF;'>" + token + "</h3>"
                    + "<p>This code expires in 1 hour.</p>"
                    + "<p>If you didn't request this, ignore this email.</p>";
        return send(to, subject, body);
    }

    public boolean sendVerificationEmail(String to, String code) {
        System.out.println("[EmailService] Verification code for " + to + ": " + code);
        if (!enabled) return false;
        String subject = "Team Hub - Email Verification";
        String body = "<h2>Email Verification</h2>"
                    + "<p>Use this code to verify your email:</p>"
                    + "<h3 style='color:#00E5FF;'>" + code + "</h3>"
                    + "<p>This code expires in 1 hour.</p>";
        return send(to, subject, body);
    }

    private boolean send(String to, String subject, String body) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(fromEmail, fromPassword);
                }
            });

            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(fromEmail));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            msg.setSubject(subject);
            msg.setContent(body, "text/html; charset=utf-8");

            Transport.send(msg);
            System.out.println("[EmailService] Email sent to " + to);
            return true;
        } catch (MessagingException e) {
            System.err.println("[EmailService] Failed to send email: " + e.getMessage());
            return false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
