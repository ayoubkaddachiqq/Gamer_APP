package tn.esprit.demo.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class GmailEmailService implements EmailService {
    private volatile EmailConfig config;

    /** Eagerly constructed with a pre-loaded config (used when credentials are known). */
    public GmailEmailService(EmailConfig config) {
        this.config = config;
    }

    /** Lazily loads config on first send — safe to construct even before credentials exist. */
    public GmailEmailService() {
        this.config = null;
    }

    private EmailConfig getConfig() {
        if (config == null) {
            synchronized (this) {
                if (config == null) {
                    config = EmailConfig.load();
                }
            }
        }
        return config;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        EmailConfig cfg = getConfig();
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", cfg.getHost());
        props.put("mail.smtp.port", Integer.toString(cfg.getPort()));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(cfg.getUsername(), cfg.getPassword());
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(cfg.getFrom()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send email", e);
        }
    }
}
