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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.RiotAccount;
import tn.esprit.entities.User;
import tn.esprit.entities.UserProfile;
import tn.esprit.entities.UserRole;
import tn.esprit.services.*;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.io.InputStream;

public class ProfileController {

    private final ProfileService profileService = new ProfileService();
    private final RiotService riotService = new RiotService();
    private final GameService gameService = new GameService();
    private final AvatarService avatarService = new AvatarService();

    @FXML private ImageView avatarImage;
    @FXML private Label displayNameLabel;
    @FXML private Label usernameLabel;
    @FXML private Label emailLabel;
    @FXML private Label bioLabel;
    @FXML private Label roleLabel;
    @FXML private Label memberSinceLabel;
    @FXML private VBox riotInfoBox;
    @FXML private Label riotIdLabel;
    @FXML private Label rankLabel;
    @FXML private Label winRateLabel;
    @FXML private Label gamesCountLabel;
    @FXML private Button adminButton;

    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showError("No user logged in");
            return;
        }
        if (adminButton != null) {
            adminButton.setVisible(currentUser.getRole() == UserRole.ADMIN);
            adminButton.setManaged(currentUser.getRole() == UserRole.ADMIN);
        }
        loadProfile();
    }

    private void loadProfile() {
        UserProfile profile = profileService.getProfile(currentUser.getId());
        if (profile == null) {
            profileService.createProfile(currentUser.getId(), currentUser.getUsername());
            profile = profileService.getProfile(currentUser.getId());
        }

        String avatarPath = avatarService.getAvatarUrl(currentUser.getId());
        InputStream is = getClass().getResourceAsStream("/" + avatarPath);
        if (is != null) {
            avatarImage.setImage(new Image(is));
        } else {
            Image fallback = new Image(getClass().getResourceAsStream("/avatars/avatar1.png"));
            if (fallback != null) avatarImage.setImage(fallback);
        }

        displayNameLabel.setText(profile != null && profile.getDisplayName() != null
            ? profile.getDisplayName() : currentUser.getUsername());
        usernameLabel.setText("@" + currentUser.getUsername());
        emailLabel.setText(currentUser.getEmail());
        bioLabel.setText(profile != null && profile.getBio() != null && !profile.getBio().isEmpty()
            ? profile.getBio() : "No bio yet.");
        roleLabel.setText(currentUser.getRole().name());

        memberSinceLabel.setText(currentUser.getCreatedAt() != null
            ? currentUser.getCreatedAt().toString().substring(0, 10) : "Unknown");

        loadRiotInfo();
        loadGameCount();
    }

    private void loadRiotInfo() {
        RiotAccount riot = riotService.getRiotAccount(currentUser.getId());
        if (riot != null) {
            riotInfoBox.setVisible(true);
            riotIdLabel.setText(riot.getRiotId());
            rankLabel.setText(riot.getFormattedRank());
            winRateLabel.setText(riot.getWinRate() + "%");
        } else {
            riotInfoBox.setVisible(false);
        }
    }

    private void loadGameCount() {
        int count = gameService.getGameCount(currentUser.getId());
        gamesCountLabel.setText(String.valueOf(count));
    }

    @FXML
    private void handleEditProfile(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ProfileEdit.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 700));
        stage.setTitle("Team Hub - Edit Profile");
        stage.show();
    }

    @FXML
    private void handleGameLibrary(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/GameLibrary.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 700));
        stage.setTitle("Team Hub - Game Library");
        stage.show();
    }

    @FXML
    private void handleBackToFeed(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainInterface.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1100, 700));
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setTitle("Team Hub - E-Sport Recruitment");
        stage.show();
    }

    @FXML
    private void handleLogout(ActionEvent event) throws IOException {
        SessionManager.logout();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 700));
        stage.setTitle("Team Hub - Login");
        stage.show();
    }

    @FXML
    private void handleHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainInterface.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setTitle("Team Hub - E-Sport Recruitment");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMyPosts(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MyPosts.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setTitle("My Posts - Team Hub");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAnnonces(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Annonces.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Gestion des Annonces");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEvenements(ActionEvent event) {
        try {
            User currentUser = SessionManager.getCurrentUser();
            boolean isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;
            String fxml = isAdmin ? "/views/EvenementsAdmin.fxml" : "/views/Evenements.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Gestion des Evenements");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMarketplace(ActionEvent event) {
        try {
            User currentUser = SessionManager.getCurrentUser();
            boolean isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;
            String fxml = isAdmin ? "/views/MarketplaceAdmin.fxml" : "/views/Marketplace.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Marketplace");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Profile.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Team Hub - Profile");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAdmin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AdminDashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setTitle("Admin Dashboard - Team Hub");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        System.err.println(msg);
    }
}
