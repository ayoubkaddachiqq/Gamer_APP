package tn.esprit.demo.service;

import tn.esprit.demo.dao.AuditDao;
import tn.esprit.demo.dao.GameDao;
import tn.esprit.demo.dao.ProfileDao;
import tn.esprit.demo.dao.RiotDao;
import tn.esprit.demo.dao.TokenDao;
import tn.esprit.demo.dao.UserDao;
import tn.esprit.demo.db.DataSourceProvider;
import tn.esprit.demo.util.PasswordHasher;
import tn.esprit.demo.util.TokenGenerator;
import tn.esprit.demo.util.Validators;

import javax.sql.DataSource;

public class ServiceRegistry {
    private static AuthService authService;
    private static ResetService resetService;
    private static ProfileService profileService;
    private static GameService gameService;
    private static FaceAuthClient faceAuthClient;
    private static AvatarService avatarService;
    private static RiotService riotService;

    public static synchronized AuthService getAuthService() {
        if (authService == null) {
            DataSource dataSource = DataSourceProvider.getDataSource();
            UserDao userDao = new UserDao(dataSource);
            ProfileDao profileDao = new ProfileDao(dataSource);
            TokenDao emailTokenDao = new TokenDao(dataSource, "email_verification_tokens");
            AuditDao auditDao = new AuditDao(dataSource);
            PasswordHasher passwordHasher = new PasswordHasher();
            Validators validators = new Validators();
            TokenGenerator tokenGenerator = new TokenGenerator();
            EmailService emailService = new GmailEmailService();

            authService = new AuthService(userDao, profileDao, emailTokenDao, emailService,
                    auditDao, passwordHasher, validators, tokenGenerator);
        }
        return authService;
    }

    public static synchronized ResetService getResetService() {
        if (resetService == null) {
            DataSource dataSource = DataSourceProvider.getDataSource();
            UserDao userDao = new UserDao(dataSource);
            TokenDao resetTokenDao = new TokenDao(dataSource, "password_reset_tokens");
            AuditDao auditDao = new AuditDao(dataSource);
            PasswordHasher passwordHasher = new PasswordHasher();
            TokenGenerator tokenGenerator = new TokenGenerator();
            Validators validators = new Validators();
            EmailService emailService = new GmailEmailService();

            resetService = new ResetService(userDao, resetTokenDao, emailService, auditDao,
                    passwordHasher, tokenGenerator, validators);
        }
        return resetService;
    }

    public static synchronized ProfileService getProfileService() {
        if (profileService == null) {
            DataSource dataSource = DataSourceProvider.getDataSource();
            ProfileDao profileDao = new ProfileDao(dataSource);
            UserDao userDao = new UserDao(dataSource);
            Validators validators = new Validators();
            profileService = new ProfileService(profileDao, userDao, validators);
        }
        return profileService;
    }

    public static synchronized GameService getGameService() {
        if (gameService == null) {
            DataSource dataSource = DataSourceProvider.getDataSource();
            GameDao gameDao = new GameDao(dataSource);
            ApiConfig apiConfig = ApiConfig.load();
            gameService = new GameService(gameDao, apiConfig);
        }
        return gameService;
    }

    public static synchronized FaceAuthClient getFaceAuthClient() {
        if (faceAuthClient == null) {
            faceAuthClient = new FaceAuthClient();
        }
        return faceAuthClient;
    }

    public static synchronized AvatarService getAvatarService() {
        if (avatarService == null) {
            ApiConfig cfg = ApiConfig.load();
            avatarService = new AvatarService(cfg.getImgbbApiKey(), cfg.getLightxApiKey());
        }
        return avatarService;
    }

    public static synchronized RiotService getRiotService() {
        if (riotService == null) {
            DataSource dataSource = DataSourceProvider.getDataSource();
            RiotDao riotDao = new RiotDao(dataSource);
            ApiConfig cfg = ApiConfig.load();
            riotService = new RiotService(riotDao, cfg.getRiotApiKey());
        }
        return riotService;
    }
}


