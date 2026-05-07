package tn.esprit.demo.service;

import tn.esprit.demo.dao.ProfileDao;
import tn.esprit.demo.dao.UserDao;
import tn.esprit.demo.model.UserProfile;
import tn.esprit.demo.util.Validators;

public class ProfileService {
    private final ProfileDao profileDao;
    private final UserDao userDao;
    private final Validators validators;

    public ProfileService(ProfileDao profileDao, UserDao userDao, Validators validators) {
        this.profileDao = profileDao;
        this.userDao = userDao;
        this.validators = validators;
    }

    public UserProfile loadProfile(long userId) {
        return profileDao.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfile profile = new UserProfile();
                    profile.setUserId(userId);
                    profileDao.upsert(profile);
                    return profile;
                });
    }

    public void updateProfile(long userId, String email, String username, String displayName, String bio, String avatarUrl) {
        if (!validators.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email address");
        }
        if (!validators.isValidUsername(username)) {
            throw new IllegalArgumentException("Invalid username");
        }

        userDao.updateBasicInfo(userId, email.trim().toLowerCase(), username.trim());

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setDisplayName(displayName == null ? null : displayName.trim());
        profile.setBio(bio == null ? null : bio.trim());
        profile.setAvatarUrl(avatarUrl == null ? null : avatarUrl.trim());
        profileDao.upsert(profile);
    }

    /**
     * Persists just the avatar URL for a user without touching any other fields.
     * Called after successful AI avatar generation.
     */
    public void saveAvatarUrl(long userId, String avatarUrl) {
        profileDao.updateAvatarUrl(userId, avatarUrl == null ? null : avatarUrl.trim());
    }
}

