package tn.esprit.demo.service;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
}
