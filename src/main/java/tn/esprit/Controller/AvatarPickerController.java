package tn.esprit.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.AvatarService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.io.InputStream;

public class AvatarPickerController {

    private final AvatarService avatarService = new AvatarService();
    private String selectedAvatar;

    @FXML private GridPane avatarGrid;
    @FXML private ImageView previewImage;
    @FXML private Label selectedLabel;

    private static final String[] AVATARS = {
        "avatars/avatar1.png", "avatars/avatar2.png", "avatars/avatar3.png", "avatars/avatar4.png",
        "avatars/avatar5.png", "avatars/avatar6.png", "avatars/avatar7.png", "avatars/avatar8.png"
    };

    @FXML
    public void initialize() {
        int col = 0;
        int row = 0;
        for (String avatar : AVATARS) {
            InputStream is = getClass().getResourceAsStream("/" + avatar);
            if (is == null) continue;

            ImageView view = new ImageView(new Image(is));
            view.setFitWidth(80);
            view.setFitHeight(80);
            view.setPreserveRatio(true);
            view.setStyle("-fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 3; -fx-border-radius: 10;");
            view.setUserData(avatar);

            view.setOnMouseClicked(this::handleAvatarClick);
            avatarGrid.add(view, col, row);

            col++;
            if (col >= 4) {
                col = 0;
                row++;
            }
        }
    }

    private void handleAvatarClick(MouseEvent event) {
        ImageView clicked = (ImageView) event.getSource();
        selectedAvatar = (String) clicked.getUserData();

        for (Node node : avatarGrid.getChildren()) {
            if (node instanceof ImageView) {
                node.setStyle("-fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 3; -fx-border-radius: 10;");
            }
        }
        clicked.setStyle("-fx-cursor: hand; -fx-border-color: #00E5FF; -fx-border-width: 3; -fx-border-radius: 10;");

        InputStream is = getClass().getResourceAsStream("/" + selectedAvatar);
        if (is != null) {
            previewImage.setImage(new Image(is));
        }
        selectedLabel.setText("Selected: " + selectedAvatar);
    }

    @FXML
    private void handleConfirm(ActionEvent event) throws IOException {
        if (selectedAvatar == null) return;

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            avatarService.updateAvatar(currentUser.getId(), selectedAvatar);
        }

        navigateBack(event);
    }

    @FXML
    private void handleCancel(ActionEvent event) throws IOException {
        navigateBack(event);
    }

    private void navigateBack(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ProfileEdit.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 700));
        stage.setTitle("Team Hub - Edit Profile");
        stage.setMaximized(true);
        stage.show();
    }
}
