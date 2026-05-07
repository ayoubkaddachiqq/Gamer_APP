package tn.esprit.demo.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tn.esprit.demo.model.RiotAccount;
import tn.esprit.demo.model.User;
import tn.esprit.demo.model.UserProfile;
import tn.esprit.demo.service.AvatarService;
import tn.esprit.demo.service.ProfileService;
import tn.esprit.demo.service.RiotService;
import tn.esprit.demo.service.ServiceRegistry;
import tn.esprit.demo.service.SessionManager;

import java.io.File;
import java.io.IOException;

public class ProfileEditController {

    // ── Form fields ───────────────────────────────────────────────────────────
    @FXML private TextField  displayNameField;
    @FXML private TextField  usernameField;
    @FXML private TextField  emailField;
    @FXML private TextArea   bioField;
    @FXML private Label      statusLabel;

    // ── Avatar widgets ────────────────────────────────────────────────────────
    @FXML private StackPane  avatarPreviewPane;
    @FXML private Label      avatarPreviewLabel;
    @FXML private ImageView  avatarPreviewImage;

    // ── AI avatar widgets ─────────────────────────────────────────────────────
    @FXML private Button     generateAiAvatarBtn;
    @FXML private Label      aiStatusLabel;
    @FXML private Button     saveAiAvatarBtn;

    // ── Riot account widgets ───────────────────────────────────────────────────
    @FXML private TextField  riotGameNameField;
    @FXML private TextField  riotTagLineField;
    @FXML private Button     linkRiotBtn;
    @FXML private Button     unlinkRiotBtn;
    @FXML private Button     saveRiotBtn;
    @FXML private Button     useLolAvatarBtn;
    @FXML private Label      riotStatusLabel;
    @FXML private VBox       riotPreviewBox;
    @FXML private Label      riotPreviewName;
    @FXML private Label      riotPreviewRank;
    @FXML private Label      riotPreviewStats;
    @FXML private ImageView  lolIconView;
    @FXML private Label      lolIconPlaceholder;

    // ── State ─────────────────────────────────────────────────────────────────
    private String      pendingAvatarUrl;
    private String      generatedAvatarUrl;
    private RiotAccount pendingRiotAccount;   // fetched but not yet saved

    // ── Services ──────────────────────────────────────────────────────────────
    private final ProfileService profileService = ServiceRegistry.getProfileService();
    private final AvatarService  avatarService  = ServiceRegistry.getAvatarService();
    private final RiotService    riotService    = ServiceRegistry.getRiotService();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            statusLabel.setText("No active session. Please login again.");
            return;
        }
        UserProfile profile = profileService.loadProfile(user.getId());
        displayNameField.setText(profile.getDisplayName() != null ? profile.getDisplayName() : "");
        usernameField.setText(user.getUsername());
        emailField.setText(user.getEmail());
        bioField.setText(profile.getBio() != null ? profile.getBio() : "");

        pendingAvatarUrl = profile.getAvatarUrl();
        String displayName = profile.getDisplayName() != null ? profile.getDisplayName() : user.getUsername();
        String initials    = displayName.length() >= 2 ? displayName.substring(0, 2).toUpperCase() : "TH";
        refreshAvatarPreview(pendingAvatarUrl, initials);

        // AI section starts hidden
        aiStatusLabel.setVisible(false);   aiStatusLabel.setManaged(false);
        saveAiAvatarBtn.setVisible(false); saveAiAvatarBtn.setManaged(false);

        // Riot section: load existing account if any
        initRiotSection(user.getId());
    }

    // ── Standard avatar picker ─────────────────────────────────────────────────

    @FXML
    private void handleChangeAvatar(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/tn/esprit/demo/view/avatar-picker.fxml"));
        Parent pickerRoot = loader.load();
        AvatarPickerController pickerCtrl = loader.getController();

        Stage pickerStage = new Stage(StageStyle.UNDECORATED);
        pickerStage.initModality(Modality.APPLICATION_MODAL);
        pickerStage.initOwner(((Node) event.getSource()).getScene().getWindow());
        pickerStage.setScene(new Scene(pickerRoot));

        pickerCtrl.setOnAvatarChosen(chosenPath -> {
            pendingAvatarUrl = chosenPath;
            String dn      = displayNameField.getText().trim();
            String initials = dn.length() >= 2 ? dn.substring(0, 2).toUpperCase() : "TH";
            refreshAvatarPreview(pendingAvatarUrl, initials);
            hideAiResult();
        });
        pickerStage.showAndWait();
    }

    // ── AI Avatar ──────────────────────────────────────────────────────────────

    @FXML
    private void handleGenerateAiAvatar(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select a photo for your AI Avatar");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.webp"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File selectedFile = chooser.showOpenDialog(stage);
        if (selectedFile == null) return;

        if (selectedFile.length() > 5 * 1024 * 1024L) {
            showAiError("Image too large. Please pick a file under 5 MB.");
            return;
        }

        setAiLoadingState(true);
        hideAiResult();

        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                return avatarService.generateAvatar(selectedFile);
            }
        };
        task.setOnSucceeded(e -> { generatedAvatarUrl = task.getValue(); setAiLoadingState(false); showAiSuccess(generatedAvatarUrl); });
        task.setOnFailed(e -> { setAiLoadingState(false);
            Throwable cause = task.getException();
            showAiError(cause instanceof AvatarService.AvatarException ? cause.getMessage()
                    : "Avatar generation failed. Check your connection and try again."); });
        new Thread(task, "ai-avatar-gen").start();
    }

    @FXML
    private void handleSaveAiAvatar(ActionEvent event) {
        if (generatedAvatarUrl == null || generatedAvatarUrl.isBlank()) return;
        User user = SessionManager.getCurrentUser();
        if (user == null) { showAiError("No active session."); return; }
        try {
            profileService.saveAvatarUrl(user.getId(), generatedAvatarUrl);
            pendingAvatarUrl = generatedAvatarUrl;
            statusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px;");
            statusLabel.setText("AI avatar saved to your profile!");
            showAiLabel("✅  Avatar saved! Reflected on your profile.", "#22c55e");
        } catch (Exception ex) { showAiError("Failed to save avatar: " + ex.getMessage()); }
    }

    // ── Riot account ───────────────────────────────────────────────────────────

    private void initRiotSection(long userId) {
        hideRiotPreview();
        RiotAccount existing = riotService.loadAccount(userId);
        if (existing != null) {
            riotGameNameField.setText(existing.getGameName());
            riotTagLineField.setText(existing.getTagLine());
            showRiotPreview(existing);
            unlinkRiotBtn.setVisible(true); unlinkRiotBtn.setManaged(true);
            showRiotLabel("✅  Account linked: " + existing.getRiotId(), "#22c55e");
        } else {
            unlinkRiotBtn.setVisible(false); unlinkRiotBtn.setManaged(false);
        }
        saveRiotBtn.setVisible(false); saveRiotBtn.setManaged(false);
    }

    @FXML
    private void handleLinkRiot(ActionEvent event) {
        String gameName = riotGameNameField.getText().trim();
        String tagLine  = riotTagLineField.getText().trim();
        if (gameName.isEmpty() || tagLine.isEmpty()) {
            showRiotLabel("⚠  Please enter both Game Name and Tag.", "#ff4d4d");
            return;
        }

        linkRiotBtn.setDisable(true);
        linkRiotBtn.setText("⏳  Fetching stats...");
        showRiotLabel("Fetching your stats...", "#00E5FF");
        hideRiotPreview();
        saveRiotBtn.setVisible(false); saveRiotBtn.setManaged(false);

        Task<RiotAccount> task = new Task<>() {
            @Override protected RiotAccount call() throws Exception {
                return riotService.fetchAccount(gameName, tagLine);
            }
        };
        task.setOnSucceeded(e -> {
            pendingRiotAccount = task.getValue();
            linkRiotBtn.setDisable(false);
            linkRiotBtn.setText("🔗  Link Account");
            showRiotPreview(pendingRiotAccount);
            showRiotLabel("✅  Found! Click Save to link this account.", "#22c55e");
            saveRiotBtn.setVisible(true); saveRiotBtn.setManaged(true);
            unlinkRiotBtn.setVisible(true); unlinkRiotBtn.setManaged(true);
        });
        task.setOnFailed(e -> {
            linkRiotBtn.setDisable(false);
            linkRiotBtn.setText("🔗  Link Account");
            Throwable cause = task.getException();
            showRiotLabel("⚠  " + (cause != null ? cause.getMessage() : "Unknown error."), "#ff4d4d");
        });
        new Thread(task, "riot-fetch").start();
    }

    @FXML
    private void handleSaveRiot(ActionEvent event) {
        if (pendingRiotAccount == null) return;
        User user = SessionManager.getCurrentUser();
        if (user == null) return;
        try {
            riotService.saveAccount(user.getId(), pendingRiotAccount);
            statusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px;");
            statusLabel.setText("Riot account linked successfully!");
            showRiotLabel("✅  Saved! Your profile now shows your LoL stats.", "#22c55e");
            saveRiotBtn.setVisible(false); saveRiotBtn.setManaged(false);
        } catch (Exception ex) {
            // Unwrap to get the real SQL error message (IllegalStateException wraps SQLException)
            String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
            showRiotLabel("⚠  Save failed: " + msg, "#ff4d4d");
        }
    }

    @FXML
    private void handleUnlinkRiot(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;
        try {
            riotService.unlinkAccount(user.getId());
            pendingRiotAccount = null;
            riotGameNameField.clear();
            riotTagLineField.clear();
            hideRiotPreview();
            saveRiotBtn.setVisible(false);   saveRiotBtn.setManaged(false);
            unlinkRiotBtn.setVisible(false); unlinkRiotBtn.setManaged(false);
            showRiotLabel("Account unlinked.", "#5a6478");
        } catch (Exception ex) {
            showRiotLabel("⚠  Unlink failed: " + ex.getMessage(), "#ff4d4d");
        }
    }

    private void showRiotPreview(RiotAccount a) {
        riotPreviewName.setText(a.getRiotId() + "  ·  Level " + a.getSummonerLevel());
        riotPreviewRank.setText(a.getFormattedRank() + "  —  " + a.getLeaguePoints() + " LP");
        riotPreviewRank.setStyle("-fx-text-fill: " + tierColor(a.getTier()) + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        riotPreviewStats.setText(a.getWins() + "W  " + a.getLosses() + "L  ·  " + riotService.getWinRate(a.getWins(), a.getLosses()) + " WR");
        riotPreviewBox.setVisible(true); riotPreviewBox.setManaged(true);

        // Load LoL profile icon from Data Dragon CDN
        if (a.getProfileIconId() > 0) {
            useLolAvatarBtn.setVisible(true); useLolAvatarBtn.setManaged(true);
            int iconId = a.getProfileIconId();
            Task<Image> iconTask = new Task<>() {
                @Override protected Image call() throws Exception {
                    String url = riotService.buildProfileIconUrl(iconId);
                    return new Image(url, 52, 52, false, true, false);
                }
            };
            iconTask.setOnSucceeded(e -> {
                Image img = iconTask.getValue();
                if (img != null && !img.isError()) {
                    lolIconView.setImage(img);
                    javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(26, 26, 26);
                    lolIconView.setClip(clip);
                    lolIconView.setVisible(true);
                    lolIconPlaceholder.setVisible(false); lolIconPlaceholder.setManaged(false);
                }
            });
            new Thread(iconTask, "lol-icon-load").start();
        } else {
            useLolAvatarBtn.setVisible(false); useLolAvatarBtn.setManaged(false);
        }
    }

    private void hideRiotPreview() {
        riotPreviewBox.setVisible(false); riotPreviewBox.setManaged(false);
        useLolAvatarBtn.setVisible(false); useLolAvatarBtn.setManaged(false);
    }

    private void showRiotLabel(String text, String color) {
        riotStatusLabel.setText(text);
        riotStatusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px;");
        riotStatusLabel.setVisible(true); riotStatusLabel.setManaged(true);
    }

    /**
     * Saves the LoL profile icon (Data Dragon CDN URL) as the user's avatar
     * and refreshes the main avatar preview in the form.
     */
    @FXML
    private void handleUseLolAvatar(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        // Determine which account to use: pending (just linked) or stored
        RiotAccount account = pendingRiotAccount;
        if (account == null) account = riotService.loadAccount(user.getId());
        if (account == null || account.getProfileIconId() <= 0) {
            showRiotLabel("⚠  No LoL icon available.", "#ff4d4d"); return;
        }

        useLolAvatarBtn.setDisable(true);
        final RiotAccount finalAccount = account;
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                return riotService.buildProfileIconUrl(finalAccount.getProfileIconId());
            }
        };
        task.setOnSucceeded(e -> {
            String iconUrl = task.getValue();
            try {
                profileService.saveAvatarUrl(user.getId(), iconUrl);
                pendingAvatarUrl = iconUrl;
                // Update main avatar preview
                String name = displayNameField.getText().trim();
                String initials = name.length() >= 2 ? name.substring(0, 2).toUpperCase() : "TH";
                refreshAvatarPreview(iconUrl, initials);
                showRiotLabel("✅  LoL icon set as your avatar!", "#22c55e");
                statusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px;");
                statusLabel.setText("Avatar updated from LoL profile icon.");
            } catch (Exception ex) {
                showRiotLabel("⚠  Could not save avatar: " + ex.getMessage(), "#ff4d4d");
            } finally {
                useLolAvatarBtn.setDisable(false);
            }
        });
        task.setOnFailed(e -> {
            useLolAvatarBtn.setDisable(false);
            showRiotLabel("⚠  Could not load LoL icon.", "#ff4d4d");
        });
        new Thread(task, "lol-avatar-apply").start();
    }

    // ── AI helpers ─────────────────────────────────────────────────────────────

    private void setAiLoadingState(boolean loading) {
        generateAiAvatarBtn.setDisable(loading);
        generateAiAvatarBtn.setText(loading ? "⏳  Generating your avatar..." : "✨  Generate AI Avatar");
        if (loading) { showAiLabel("Uploading and cartoonifying your photo…", "#00E5FF");
            saveAiAvatarBtn.setVisible(false); saveAiAvatarBtn.setManaged(false); }
    }

    private void showAiSuccess(String url) {
        Task<Image> imgTask = new Task<>() { @Override protected Image call() { return new Image(url, 64, 64, false, true, false); } };
        imgTask.setOnSucceeded(e -> {
            Image img = imgTask.getValue();
            if (img != null && !img.isError()) {
                avatarPreviewImage.setFitWidth(64); avatarPreviewImage.setFitHeight(64);
                avatarPreviewImage.setPreserveRatio(false);
                avatarPreviewImage.setClip(new Circle(32, 32, 32));
                avatarPreviewImage.setImage(img);
                avatarPreviewImage.setVisible(true); avatarPreviewImage.setManaged(true);
                avatarPreviewLabel.setVisible(false); avatarPreviewLabel.setManaged(false);
            }
        });
        new Thread(imgTask, "ai-img-load").start();
        showAiLabel("🎨  Avatar ready! Click Save Avatar to apply it.", "#a78bfa");
        saveAiAvatarBtn.setVisible(true); saveAiAvatarBtn.setManaged(true);
    }

    private void showAiError(String msg) { showAiLabel("⚠  " + msg, "#ff4d4d"); saveAiAvatarBtn.setVisible(false); saveAiAvatarBtn.setManaged(false); }
    private void showAiLabel(String t, String c) {
        aiStatusLabel.setText(t); aiStatusLabel.setStyle("-fx-text-fill:" + c + ";-fx-font-size:11px;");
        aiStatusLabel.setVisible(true); aiStatusLabel.setManaged(true);
    }
    private void hideAiResult() { aiStatusLabel.setVisible(false); aiStatusLabel.setManaged(false); saveAiAvatarBtn.setVisible(false); saveAiAvatarBtn.setManaged(false); generatedAvatarUrl = null; }

    // ── Avatar preview ─────────────────────────────────────────────────────────

    private void refreshAvatarPreview(String avatarUrl, String initials) {
        ProfileController.applyAvatarToImageView(avatarUrl, initials, avatarPreviewLabel, avatarPreviewImage);
    }

    // ── Save / Cancel ──────────────────────────────────────────────────────────

    @FXML
    private void handleSave(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user == null) { statusLabel.setText("No active session."); return; }
        if (generatedAvatarUrl != null && !generatedAvatarUrl.isBlank()) pendingAvatarUrl = generatedAvatarUrl;
        try {
            profileService.updateProfile(user.getId(), emailField.getText(), usernameField.getText(),
                    displayNameField.getText(), bioField.getText(), pendingAvatarUrl);
            statusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px;");
            statusLabel.setText("Profile updated successfully.");
        } catch (Exception ex) {
            statusLabel.setStyle("-fx-text-fill: #ff4d4d; -fx-font-size: 11px;");
            statusLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) throws IOException { goToProfile(event); }

    private void goToProfile(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/demo/view/profile.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("TeamHub – Profile");
        stage.setScene(new Scene(root, 1200, 750));
        stage.show();
    }

    // ── Tier colour map ────────────────────────────────────────────────────────

    static String tierColor(String tier) {
        if (tier == null) return "#5a6478";
        return switch (tier.toUpperCase()) {
            case "IRON"                         -> "#8b8b8b";
            case "BRONZE"                       -> "#cd7f32";
            case "SILVER"                       -> "#c0c0c0";
            case "GOLD"                         -> "#FFD700";
            case "PLATINUM"                     -> "#00b4b4";
            case "EMERALD"                      -> "#22c55e";
            case "DIAMOND"                      -> "#7dd3fc";
            case "MASTER"                       -> "#a78bfa";
            case "GRANDMASTER"                  -> "#f87171";
            case "CHALLENGER"                   -> "#f59e0b";
            default                             -> "#dde4f0";
        };
    }
}
