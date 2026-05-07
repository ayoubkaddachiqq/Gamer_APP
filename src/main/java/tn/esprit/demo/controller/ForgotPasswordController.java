package tn.esprit.demo.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.demo.service.ResetService;
import tn.esprit.demo.service.ServiceRegistry;

import java.io.IOException;

public class ForgotPasswordController {
    @FXML
    private TextField emailField;

    @FXML
    private TextField codeField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private Label statusLabel;

    private final ResetService resetService = ServiceRegistry.getResetService();

    @FXML
    private void handleSendCode(ActionEvent event) {
        try {
            resetService.requestReset(emailField.getText(), "local");
            statusLabel.setText("Verification code sent. Check your email.");
        } catch (Exception ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void handleResetPassword(ActionEvent event) {
        try {
            resetService.resetPassword(emailField.getText(), codeField.getText(), newPasswordField.getText());
            statusLabel.setText("Password updated. You can sign in now.");
        } catch (Exception ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void goToLogin(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/tn/esprit/demo/view/login.fxml")
        );
        Parent root = loader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("TeamHub – Login");
        stage.setScene(new Scene(root, 420, 400));
        stage.show();
    }
}
