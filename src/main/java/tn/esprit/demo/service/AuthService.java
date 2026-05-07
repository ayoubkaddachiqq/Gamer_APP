package tn.esprit.demo.service;

import tn.esprit.demo.dao.AuditDao;
import tn.esprit.demo.dao.ProfileDao;
import tn.esprit.demo.dao.TokenDao;
import tn.esprit.demo.dao.UserDao;
import tn.esprit.demo.model.TokenRecord;
import tn.esprit.demo.model.User;
import tn.esprit.demo.model.UserProfile;
import tn.esprit.demo.model.UserRole;
import tn.esprit.demo.model.UserStatus;
import tn.esprit.demo.util.PasswordHasher;
import tn.esprit.demo.util.TokenGenerator;
import tn.esprit.demo.util.Validators;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class AuthService {
    private final UserDao userDao;
    private final ProfileDao profileDao;
    private final TokenDao emailTokenDao;
    private final EmailService emailService;
    private final AuditDao auditDao;
    private final PasswordHasher passwordHasher;
    private final Validators validators;
    private final TokenGenerator tokenGenerator;

    public AuthService(UserDao userDao,
                       ProfileDao profileDao,
                       TokenDao emailTokenDao,
                       EmailService emailService,
                       AuditDao auditDao,
                       PasswordHasher passwordHasher,
                       Validators validators,
                       TokenGenerator tokenGenerator) {
        this.userDao = userDao;
        this.profileDao = profileDao;
        this.emailTokenDao = emailTokenDao;
        this.emailService = emailService;
        this.auditDao = auditDao;
        this.passwordHasher = passwordHasher;
        this.validators = validators;
        this.tokenGenerator = tokenGenerator;
    }

    public User register(String username, String email, String password, UserRole role) {
        if (!validators.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email address");
        }
        if (!validators.isValidUsername(username)) {
            throw new IllegalArgumentException("Invalid username");
        }
        if (!validators.isValidPassword(password)) {
            throw new IllegalArgumentException("Password must contain upper, lower, digit and be 8+ chars");
        }
        if (userDao.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setEmail(email.trim().toLowerCase());
        user.setUsername(username.trim());
        user.setPasswordHash(passwordHasher.hash(password));
        user.setRole(role == null ? UserRole.PLAYER : role);
        user.setStatus(UserStatus.PENDING);
        user.setEmailVerified(false);

        long userId = userDao.create(user);
        user.setId(userId);

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setDisplayName(username.trim());
        profileDao.upsert(profile);

        sendEmailVerification(user);
        auditDao.log(userId, "REGISTER", "Registered with email");
        return user;
    }

    public User login(String email, String password) {
        Optional<User> optionalUser = userDao.findByEmail(email.trim().toLowerCase());
        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        User user = optionalUser.get();
        if (!passwordHasher.matches(password, user.getPasswordHash())) {
            auditDao.log(user.getId(), "LOGIN_FAILED", "Invalid password");
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new IllegalArgumentException("Account is locked");
        }

        SessionManager.setCurrentUser(user);
        profileDao.updateLastLogin(user.getId(), Instant.now());
        auditDao.log(user.getId(), "LOGIN_SUCCESS", "Logged in");
        return user;
    }

    public void sendEmailVerification(User user) {
        String code = tokenGenerator.generateSixDigitCode();
        TokenRecord token = new TokenRecord();
        token.setUserId(user.getId());
        token.setCodeHash(passwordHasher.hash(code));
        token.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        emailTokenDao.create(token);

        String subject = "TeamHub verification code";
        String body = "Your verification code is: " + code + "\nIt expires in 15 minutes.";
        emailService.sendEmail(user.getEmail(), subject, body);
    }

    public void verifyEmail(long userId, String code) {
        TokenRecord token = emailTokenDao.findActiveByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No active verification code"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Verification code expired");
        }

        if (!passwordHasher.matches(code, token.getCodeHash())) {
            throw new IllegalArgumentException("Invalid verification code");
        }

        emailTokenDao.markUsed(token.getId());
        userDao.updateStatus(userId, UserStatus.ACTIVE, true);
        auditDao.log(userId, "EMAIL_VERIFIED", "Email verification success");
    }

    /**
     * Authenticates a user whose identity has already been verified by Face ID.
     * Skips password check — the face recognition service is the credential.
     *
     * @param email the email address returned by the face recognition service
     * @return the authenticated {@link User}
     * @throws IllegalArgumentException if the account does not exist or is locked
     */
    public User loginByFaceId(String email) {
        Optional<User> optionalUser = userDao.findByEmail(email.trim().toLowerCase());
        if (optionalUser.isEmpty()) {
            throw new IllegalArgumentException("Face recognised but no matching account found");
        }

        User user = optionalUser.get();
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new IllegalArgumentException("Account is locked");
        }

        SessionManager.setCurrentUser(user);
        profileDao.updateLastLogin(user.getId(), Instant.now());
        auditDao.log(user.getId(), "LOGIN_FACE_ID", "Authenticated via Face ID");
        return user;
    }
}

