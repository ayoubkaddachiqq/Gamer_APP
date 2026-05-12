package tn.esprit.entities;

import java.time.Instant;

public class User {
    private int id;
    private String email;
    private String username;
    private String passwordHash;
    private UserRole role;
    private UserStatus status;
    private boolean emailVerified;
    private boolean faceEnrolled;
    private String profilePhoto;
    private Instant createdAt;
    private Instant updatedAt;

    public User() {}

    public User(int id, String email, String username, String passwordHash, UserRole role, UserStatus status) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isFaceEnrolled() { return faceEnrolled; }
    public void setFaceEnrolled(boolean faceEnrolled) { this.faceEnrolled = faceEnrolled; }

    public String getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
