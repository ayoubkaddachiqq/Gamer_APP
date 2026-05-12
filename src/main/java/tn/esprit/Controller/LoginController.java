package tn.esprit.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.AuthService;

import java.io.IOException;

public class LoginController {

    private final AuthService authService = new AuthService();

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

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
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        clearError();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password.");
            return;
        }

        try {
            User user = authService.login(email, password);
            navigateToMain(event);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void goToRegister(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Register.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Team Hub - Register");
        stage.setScene(new Scene(root, 1000, 700));
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.show();
    }

    private void navigateToMain(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainInterface.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Team Hub - E-Sport Recruitment");
        stage.setScene(new Scene(root, 1100, 700));
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
    }
}
