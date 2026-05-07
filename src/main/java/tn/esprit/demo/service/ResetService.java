package tn.esprit.demo.service;

import tn.esprit.demo.dao.AuditDao;
import tn.esprit.demo.dao.TokenDao;
import tn.esprit.demo.dao.UserDao;
import tn.esprit.demo.model.TokenRecord;
import tn.esprit.demo.model.User;
import tn.esprit.demo.util.PasswordHasher;
import tn.esprit.demo.util.TokenGenerator;
import tn.esprit.demo.util.Validators;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class ResetService {
    private final UserDao userDao;
    private final TokenDao resetTokenDao;
    private final EmailService emailService;
    private final AuditDao auditDao;
    private final PasswordHasher passwordHasher;
    private final TokenGenerator tokenGenerator;
    private final Validators validators;

    public ResetService(UserDao userDao,
                        TokenDao resetTokenDao,
                        EmailService emailService,
                        AuditDao auditDao,
                        PasswordHasher passwordHasher,
                        TokenGenerator tokenGenerator,
                        Validators validators) {
        this.userDao = userDao;
        this.resetTokenDao = resetTokenDao;
        this.emailService = emailService;
        this.auditDao = auditDao;
        this.passwordHasher = passwordHasher;
        this.tokenGenerator = tokenGenerator;
        this.validators = validators;
    }

    public void requestReset(String email, String requestIp) {
        User user = userDao.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));

        String code = tokenGenerator.generateSixDigitCode();
        TokenRecord token = new TokenRecord();
        token.setUserId(user.getId());
        token.setCodeHash(passwordHasher.hash(code));
        token.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        token.setRequestIp(requestIp);
        resetTokenDao.create(token);

        String subject = "TeamHub password reset code";
        String body = "Your reset code is: " + code + "\nIt expires in 10 minutes.";
        emailService.sendEmail(user.getEmail(), subject, body);
        auditDao.log(user.getId(), "PASSWORD_RESET_REQUEST", "Reset code sent");
    }

    public void resetPassword(String email, String code, String newPassword) {
        if (!validators.isValidPassword(newPassword)) {
            throw new IllegalArgumentException("Password must contain upper, lower, digit and be 8+ chars");
        }
        User user = userDao.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));
        TokenRecord token = resetTokenDao.findActiveByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No active reset code"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Reset code expired");
        }

        if (!passwordHasher.matches(code, token.getCodeHash())) {
            throw new IllegalArgumentException("Invalid reset code");
        }

        resetTokenDao.markUsed(token.getId());
        userDao.updatePassword(user.getId(), passwordHasher.hash(newPassword));
        auditDao.log(user.getId(), "PASSWORD_RESET", "Password reset completed");
    }
}
