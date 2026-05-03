package org.esprit.utils;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.util.Duration;

public final class UiEffects {

    private UiEffects() {
    }

    public static void applyEntranceAndHover(Parent root) {
        if (root == null) {
            return;
        }

        root.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(260), root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        for (Node node : root.lookupAll(".button")) {
            if (node instanceof Button button) {
                addScaleHover(button);
            }
        }
    }

    private static void addScaleHover(Node node) {
        node.setOnMouseEntered(event -> scale(node, 1.035));
        node.setOnMouseExited(event -> scale(node, 1.0));
    }

    private static void scale(Node node, double value) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(120), node);
        transition.setToX(value);
        transition.setToY(value);
        transition.play();
    }
}
