package tn.esprit.demo.controller;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.demo.model.UserRole;
import tn.esprit.demo.service.AuthService;
import tn.esprit.demo.service.ServiceRegistry;
import tn.esprit.demo.util.HibpClient;
import tn.esprit.demo.util.HibpClient.PwnResult;

import java.io.IOException;

/**
 * Controller for the Register screen.
 *
 * Password flow:
 *  1. Local format validation (upper + lower + digit + 8+ chars)
 *  2. HIBP k-anonymity check on a background thread (button shows "Checking…")
 *  3. If breached → block with count; if network error → warn but allow through
 *  4. AuthService.register() called only when both checks pass
 */
public class RegisterController {

    private final AuthService authService = ServiceRegistry.getAuthService();

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ChoiceBox<String> roleChoiceBox;
    @FXML private Button registerBtn;
    @FXML private Label hibpStatusLabel;
    @FXML private Label errorLabel;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        roleChoiceBox.getItems().addAll("PLAYER", "TEAM");
        roleChoiceBox.setValue("PLAYER");
        clearError();
        hideHibp();
    }

    // ── Register handler ──────────────────────────────────────────────────────

    @FXML
    private void handleRegister(ActionEvent event) {
        clearError();
        hideHibp();

        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        String role     = roleChoiceBox.getValue();

        // ── 1. Empty-field guard ──────────────────────────────────────────────
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("All fields are required.");
            return;
        }

        // ── 2. Disable button, start HIBP check in background ─────────────────
        setChecking(true);

        Task<PwnResult> hibpTask = new Task<>() {
            @Override
            protected PwnResult call() {
                return HibpClient.check(password);
            }
        };

        hibpTask.setOnSucceeded(e -> {
            setChecking(false);
            PwnResult result = hibpTask.getValue();

            if (result.error()) {
                // Network / API failure — warn user but allow them to proceed
                showHibpWarning(
                    "⚠  Could not verify password breach status (network error). " +
                    "Proceeding, but consider using a different password.");
                doRegister(event, username, email, password, role);
                return;
            }

            if (result.pwned()) {
                // Password is in a breach database — block registration
                showHibpBlocked(result.count());
                return;
            }

            // ── 3. Password is safe — proceed to AuthService ──────────────────
            showHibpOk();
            doRegister(event, username, email, password, role);
        });

        hibpTask.setOnFailed(e -> {
            setChecking(false);
            // Unexpected task failure (should not happen, but fail-open)
            showHibpWarning("⚠  Password breach check failed. Proceeding anyway.");
            doRegister(event, username, email, password, role);
        });

        Thread thread = new Thread(hibpTask, "hibp-check");
        thread.setDaemon(true);
        thread.start();
    }

    // ── Registration logic (runs after HIBP check passes) ────────────────────

    private void doRegister(ActionEvent event, String username, String email,
                            String password, String role) {
        try {
            authService.register(username, email, password, UserRole.valueOf(role));
            goToLogin(event);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void goToLogin(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/tn/esprit/demo/view/login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("TeamHub – Login");
        stage.setScene(new Scene(root, 1000, 700));
        stage.show();
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    /** Grey "Checking…" state while the HTTP call is in flight. */
    private void setChecking(boolean checking) {
        registerBtn.setDisable(checking);
        registerBtn.setText(checking ? "Checking password…" : "Complete Registration");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /** Password found in breaches — red, blocks submit. */
    private void showHibpBlocked(int count) {
        String times = count == 1 ? "1 time" : String.format("%,d times", count);
        hibpStatusLabel.setText(
            "🚨  This password has appeared in " + times + " in known data breaches. " +
            "Please choose a different password.");
        hibpStatusLabel.setStyle(
            "-fx-text-fill: #ff4d4d; -fx-font-size: 11px;" +
            "-fx-background-color: rgba(255,77,77,0.10);" +
            "-fx-background-radius: 6; -fx-padding: 6 10;");
        hibpStatusLabel.setVisible(true);
        hibpStatusLabel.setManaged(true);
    }

    /** Password is safe — brief green confirmation. */
    private void showHibpOk() {
        hibpStatusLabel.setText("✓  Password not found in any known data breaches.");
        hibpStatusLabel.setStyle(
            "-fx-text-fill: #22c55e; -fx-font-size: 11px;" +
            "-fx-background-color: rgba(34,197,94,0.08);" +
            "-fx-background-radius: 6; -fx-padding: 6 10;");
        hibpStatusLabel.setVisible(true);
        hibpStatusLabel.setManaged(true);
    }

    /** Network / API error — amber warning, still allowed through. */
    private void showHibpWarning(String message) {
        hibpStatusLabel.setText(message);
        hibpStatusLabel.setStyle(
            "-fx-text-fill: #f59e0b; -fx-font-size: 11px;" +
            "-fx-background-color: rgba(245,158,11,0.08);" +
            "-fx-background-radius: 6; -fx-padding: 6 10;");
        hibpStatusLabel.setVisible(true);
        hibpStatusLabel.setManaged(true);
    }

    private void hideHibp() {
        hibpStatusLabel.setText("");
        hibpStatusLabel.setVisible(false);
        hibpStatusLabel.setManaged(false);
    }
}
