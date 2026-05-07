package tn.esprit.demo.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import tn.esprit.demo.model.User;
import tn.esprit.demo.model.UserRole;
import tn.esprit.demo.service.AuthService;
import tn.esprit.demo.service.FaceAuthClient;
import tn.esprit.demo.service.ServiceRegistry;

import java.io.IOException;

/**
 * Controller for the Login screen.
 * Package: com.teamhub.controller (mapped as tn.esprit.demo.controller)
 */
public class LoginController {

    private final AuthService    authService = ServiceRegistry.getAuthService();
    private final FaceAuthClient faceClient  = ServiceRegistry.getFaceAuthClient();

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        faceLoginButton;

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

    @FXML
    public void initialize() {
        clearError();

        // Ping the face service on a background thread — don't block the UI
        Thread pingThread = new Thread(() -> {
            boolean available = faceClient.ping();
            Platform.runLater(() -> {
                if (!available) {
                    faceLoginButton.setDisable(true);
                    faceLoginButton.setOpacity(0.45);
                    Tooltip.install(faceLoginButton, new Tooltip(
                        "Face ID service is offline.\nStart face_service/start.bat to enable."
                    ));
                }
            });
        }, "face-ping");
        pingThread.setDaemon(true);
        pingThread.start();
    }

    /**
     * Handles the password-based Sign In button click.
     */
    @FXML
    private void handleLogin(ActionEvent event) {
        clearError();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password.");
            return;
        }

        try {
            User user = authService.login(email, password);
            navigateAfterLogin(event, user);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * Handles the "Sign in with Face ID" button click.
     * Face recognition runs on a background thread so the UI stays responsive
     * while the webcam window is open.
     */
    @FXML
    private void handleFaceLogin(ActionEvent event) {
        clearError();
        faceLoginButton.setDisable(true);
        faceLoginButton.setText("⏳  Scanning...");

        Thread faceThread = new Thread(() -> {
            try {
                FaceAuthClient.RecognizeResult result = faceClient.recognize();

                Platform.runLater(() -> {
                    faceLoginButton.setDisable(false);
                    faceLoginButton.setText("👁  Sign in with Face ID");

                    if (!result.matched()) {
                        showError("Face not recognised. Try again or use your password.");
                        return;
                    }
                    try {
                        User user = authService.loginByFaceId(result.email());
                        navigateAfterLogin(event, user);
                    } catch (Exception ex) {
                        showError(ex.getMessage());
                    }
                });

            } catch (FaceAuthClient.FaceAuthException e) {
                Platform.runLater(() -> {
                    faceLoginButton.setDisable(false);
                    faceLoginButton.setText("👁  Sign in with Face ID");
                    showError(e.getMessage());
                });
            }
        }, "face-login");
        faceThread.setDaemon(true);
        faceThread.start();
    }

    /** Navigates to the Register screen. */
    @FXML
    private void goToRegister(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/tn/esprit/demo/view/register.fxml")
        );
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("TeamHub – Register");
        stage.setScene(new Scene(root, 420, 500));
        stage.show();
    }

    @FXML
    private void goToForgotPassword(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/tn/esprit/demo/view/forgot-password.fxml")
        );
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("TeamHub – Reset Password");
        stage.setScene(new Scene(root, 1000, 700));
        stage.show();
    }

    // ─── Private navigation helpers ─────────────────────────────────────────

    private void navigateAfterLogin(ActionEvent event, User user) throws IOException {
        if (user.getRole() == UserRole.ADMIN) {
            goToAdminDashboard(event);
        } else {
            goToProfile(event, user.getUsername());
        }
    }

    private void goToProfile(ActionEvent event, String username) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/tn/esprit/demo/view/profile.fxml")
        );
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("TeamHub – " + username);
        stage.setScene(new Scene(root, 1200, 750));
        stage.show();
    }

    private void goToAdminDashboard(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/tn/esprit/demo/view/admin-dashboard.fxml")
        );
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("TeamHub – Admin Dashboard");
        stage.setScene(new Scene(root, 1280, 800));
        stage.show();
    }
}
