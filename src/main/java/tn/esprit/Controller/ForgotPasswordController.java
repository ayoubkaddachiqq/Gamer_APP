package tn.esprit.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.services.EmailService;
import tn.esprit.services.PasswordResetService;

import java.io.IOException;

public class ForgotPasswordController {

    private final PasswordResetService resetService = new PasswordResetService();
    private final EmailService emailService = new EmailService();

    @FXML private TextField emailField;
    @FXML private TextField tokenField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private VBox step1Box;
    @FXML private VBox step2Box;
    @FXML private Label tokenDisplayLabel;

    private String currentToken;

    @FXML
    public void initialize() {
        step1Box.setVisible(true);
        step2Box.setVisible(false);
    }

    @FXML
    private void handleSendReset(ActionEvent event) {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showError("Please enter your email address");
            return;
        }

        String token = resetService.createResetToken(email);
        if (token != null) {
            currentToken = token;
            boolean emailed = emailService.sendResetEmail(email, token);
            step1Box.setVisible(false);
            step2Box.setVisible(true);
            tokenDisplayLabel.setText(token);
            if (emailed) {
                showSuccess("Reset code sent to " + email);
            } else {
                showSuccess("Email server not configured — use the code below:");
            }
        } else {
            showError("Email not found. Please check and try again.");
        }
    }

    @FXML
    private void handleResetPassword(ActionEvent event) {
        String token = tokenField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (token.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("All fields are required");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }

        if (newPassword.length() < 8) {
            showError("Password must be at least 8 characters");
            return;
        }

        boolean success = resetService.validateAndReset(token, newPassword);
        if (success) {
            showSuccess("Password reset successfully! You can now login.");
            goToLogin(event);
        } else {
            showError("Invalid or expired token. Please try again.");
        }
    }

    @FXML
    private void goToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Team Hub - Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }

    private void showSuccess(String msg) {
        successLabel.setText(msg);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }
}
