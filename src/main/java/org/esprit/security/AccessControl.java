package org.esprit.security;

import javafx.scene.Node;

public final class AccessControl {

    private AccessControl() {
    }

    public static void visibleForAdmin(Node... nodes) {
        setVisible(Session.isAdmin(), nodes);
    }

    public static boolean requireAdmin() {
        return Session.isAdmin();
    }

    private static void setVisible(boolean visible, Node... nodes) {
        for (Node node : nodes) {
            if (node != null) {
                node.setVisible(visible);
                node.setManaged(visible);
            }
        }
    }
}
