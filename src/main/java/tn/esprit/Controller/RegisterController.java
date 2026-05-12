package tn.esprit.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.UserRole;
import tn.esprit.services.AuthService;

import java.io.IOException;

public class RegisterController {

    private final AuthService authService = new AuthService();

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ChoiceBox<String> roleChoiceBox;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        roleChoiceBox.getItems().addAll("PLAYER", "TEAM");
        roleChoiceBox.setValue("PLAYER");
        clearError();
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

    @FXML
    private void handleRegister(ActionEvent event) {
        clearError();

        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String role = roleChoiceBox.getValue();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("All fields are required.");
            return;
        }

        try {
            authService.register(username, email, password, UserRole.valueOf(role));
            navigateToMain(event);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void goToLogin(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Team Hub - Login");
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
