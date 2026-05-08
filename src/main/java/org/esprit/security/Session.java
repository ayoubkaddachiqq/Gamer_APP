package org.esprit.security;

public final class Session {

    // Temporaire: changer ici pour tester l'affichage des ecrans.
    private static Role role = Role.ADMIN;

    private Session() {
    }

    public static Role getRole() {
        return role;
    }

    public static void setRole(Role newRole) {
        role = newRole == null ? Role.USER : newRole;
    }

    public static void setRole(String newRole) {
        role = Role.fromString(newRole);
    }

    public static boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public static boolean isUser() {
        return role == Role.USER;
    }
}
