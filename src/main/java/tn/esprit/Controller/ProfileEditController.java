package tn.esprit.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.entities.UserProfile;
import tn.esprit.services.AvatarService;
import tn.esprit.services.ProfileService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.io.InputStream;

public class ProfileEditController {

    private final ProfileService profileService = new ProfileService();
    private final AvatarService avatarService = new AvatarService();

    @FXML private ImageView avatarImage;
    @FXML private TextField displayNameField;
    @FXML private TextArea bioArea;
    @FXML private Label errorLabel;

    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        UserProfile profile = profileService.getProfile(currentUser.getId());
        if (profile == null) {
            profileService.createProfile(currentUser.getId(), currentUser.getUsername());
            profile = profileService.getProfile(currentUser.getId());
        }

        String avatarPath = avatarService.getAvatarUrl(currentUser.getId());
        InputStream is = getClass().getResourceAsStream("/" + avatarPath);
        if (is != null) {
            avatarImage.setImage(new Image(is));
        }

        if (profile != null) {
            displayNameField.setText(profile.getDisplayName());
            bioArea.setText(profile.getBio());
        }
    }

    @FXML
    private void handleSave(ActionEvent event) throws IOException {
        String displayName = displayNameField.getText().trim();
        String bio = bioArea.getText().trim();

        if (displayName.isEmpty()) {
            showError("Display name cannot be empty");
            return;
        }

        profileService.updateProfile(currentUser.getId(), displayName, bio);
        navigateBack(event);
    }

    @FXML
    private void handleChangeAvatar(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AvatarPicker.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 700, 500));
        stage.setTitle("Team Hub - Choose Avatar");
        stage.show();
    }

    @FXML
    private void handleCancel(ActionEvent event) throws IOException {
        navigateBack(event);
    }

    private void navigateBack(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Profile.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 700));
        stage.setTitle("Team Hub - Profile");
        stage.show();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }
}
