package org.esprit.utils;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.util.Duration;

public final class UiEffects {
    private static final String[] ELEVATED_SELECTORS = {
            ".glass-panel",
            ".stat-card",
            ".weather-card"
    };

    private UiEffects() {
    }

    public static void applyEntranceAndHover(Parent root) {
        if (root == null) {
            return;
        }

        root.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(320), root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        for (Node node : root.lookupAll(".button")) {
            if (node instanceof Button button) {
                addScaleHover(button);
            }
        }

        int index = 0;
        for (String selector : ELEVATED_SELECTORS) {
            for (Node node : root.lookupAll(selector)) {
                playLiftEntrance(node, index++);
                addLiftHover(node);
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
        transition.setInterpolator(Interpolator.EASE_OUT);
        transition.play();
    }

    private static void playLiftEntrance(Node node, int index) {
        node.setOpacity(0);
        node.setTranslateY(18);

        FadeTransition fade = new FadeTransition(Duration.millis(360), node);
        fade.setDelay(Duration.millis(Math.min(index * 55L, 330)));
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition lift = new TranslateTransition(Duration.millis(360), node);
        lift.setDelay(fade.getDelay());
        lift.setFromY(18);
        lift.setToY(0);
        lift.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, lift).play();
    }

    private static void addLiftHover(Node node) {
        node.setOnMouseEntered(event -> translate(node, -3));
        node.setOnMouseExited(event -> translate(node, 0));
    }

    private static void translate(Node node, double y) {
        TranslateTransition transition = new TranslateTransition(Duration.millis(140), node);
        transition.setToY(y);
        transition.setInterpolator(Interpolator.EASE_OUT);
        transition.play();
    }
}
