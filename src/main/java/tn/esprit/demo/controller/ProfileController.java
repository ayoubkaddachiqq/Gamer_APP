package tn.esprit.demo.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import tn.esprit.demo.model.MatchSummary;
import tn.esprit.demo.model.RiotAccount;
import tn.esprit.demo.model.User;
import tn.esprit.demo.model.UserGame;
import tn.esprit.demo.model.UserProfile;
import tn.esprit.demo.service.FaceAuthClient;
import tn.esprit.demo.service.GameService;
import tn.esprit.demo.service.ProfileService;
import tn.esprit.demo.service.RiotService;
import tn.esprit.demo.service.ServiceRegistry;
import tn.esprit.demo.service.SessionManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ProfileController {

    // ── Sidebar ───────────────────────────────────────────────────────────────
    @FXML private StackPane avatarPane;
    @FXML private Label     avatarLabel;
    @FXML private ImageView avatarImageView;
    @FXML private Label     gamertagLabel;
    @FXML private Label     emailLabel;

    // Sidebar rank badge
    @FXML private StackPane sidebarRankPane;
    @FXML private Label     sidebarRankBadge;

    // Sidebar quick stats (visible only when LoL linked)
    @FXML private VBox  quickStatsBox;
    @FXML private Label rankLabel;
    @FXML private Label winRateLabel;
    @FXML private Label matchesLabel;
    @FXML private Label hoursLabel;    // repurposed: summoner level

    // ── Bio ───────────────────────────────────────────────────────────────────
    @FXML private Label bioLabel;
    @FXML private Label bioPlaceholder;

    // ── LoL stats grid (hidden when not linked) ───────────────────────────────
    @FXML private HBox  statsGrid;
    @FXML private Label stat1Caption;
    @FXML private Label kdLabel;
    @FXML private Label stat1Sub;
    @FXML private Label stat2Caption;
    @FXML private Label hsLabel;
    @FXML private Label stat2Sub;
    @FXML private Label stat3Caption;
    @FXML private Label mvpLabel;
    @FXML private Label stat3Sub;
    @FXML private Label stat4Caption;
    @FXML private Label streakLabel;
    @FXML private Label stat4Sub;

    // ── Match history ─────────────────────────────────────────────────────────
    @FXML private VBox  matchList;
    @FXML private Label matchLoadingLabel;

    // ── LoL stats card ────────────────────────────────────────────────────────
    @FXML private VBox      lolCard;
    @FXML private VBox      lolNoAccount;
    @FXML private HBox      lolStats;
    @FXML private Label     tierEmojiLabel;
    @FXML private StackPane tierBadge;
    @FXML private Label     lolRankLabel;
    @FXML private Label     lolRiotIdLabel;
    @FXML private Label     lolLevelLabel;
    @FXML private Label     lolLpLabel;
    @FXML private Label     lolWlLabel;
    @FXML private Label     lolWrLabel;
    @FXML private Label     lolStatusLabel;
    @FXML private Button    refreshRiotBtn;

    // ── Game Library ──────────────────────────────────────────────────────────
    @FXML private HBox  favGamesRow;
    @FXML private Label noFavLabel;

    // ── Face ID ───────────────────────────────────────────────────────────────
    @FXML private Button enrollFaceButton;

    // ── Services ──────────────────────────────────────────────────────────────
    private final ProfileService profileService = ServiceRegistry.getProfileService();
    private final GameService    gameService    = ServiceRegistry.getGameService();
    private final FaceAuthClient faceClient     = ServiceRegistry.getFaceAuthClient();
    private final RiotService    riotService    = ServiceRegistry.getRiotService();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            gamertagLabel.setText("Guest");
            emailLabel.setText("Not signed in");
            return;
        }

        UserProfile profile = profileService.loadProfile(user.getId());
        String displayName  = profile.getDisplayName() != null
                ? profile.getDisplayName() : user.getUsername();

        // Header
        gamertagLabel.setText(displayName);
        emailLabel.setText(user.getEmail());

        // Bio
        String bio = profile.getBio();
        if (bio != null && !bio.isBlank()) {
            bioLabel.setText(bio);
            bioLabel.setVisible(true);   bioLabel.setManaged(true);
            bioPlaceholder.setVisible(false); bioPlaceholder.setManaged(false);
        } else {
            bioLabel.setVisible(false);  bioLabel.setManaged(false);
            bioPlaceholder.setVisible(true); bioPlaceholder.setManaged(true);
        }

        // Avatar
        String initials = displayName.length() >= 2
                ? displayName.substring(0, 2).toUpperCase() : "TH";
        avatarLabel.setText(initials);
        applyAvatar(profile.getAvatarUrl(), initials);

        // LoL stats (from DB, no API call)
        loadLolStats(user.getId());

        // Favourite games
        loadFavouriteGames(user.getId());
    }

    // ── LoL stats card ────────────────────────────────────────────────────────

    private void loadLolStats(long userId) {
        try {
            RiotAccount account = riotService.loadAccount(userId);
            if (account == null) {
                showLolNoAccount();
            } else {
                populateLolCard(account);
                populateSidebarStats(account);
                populateStatsGrid(account);
                // Load match history async (5 matches)
                loadMatchHistory(account.getPuuid());
            }
        } catch (Exception e) {
            System.err.println("[ProfileController] loadLolStats: " + e.getMessage());
            showLolNoAccount();
        }
    }

    private void populateLolCard(RiotAccount a) {
        lolNoAccount.setVisible(false);  lolNoAccount.setManaged(false);
        lolStats.setVisible(true);       lolStats.setManaged(true);
        refreshRiotBtn.setVisible(true); refreshRiotBtn.setManaged(true);

        String color = ProfileEditController.tierColor(a.getTier());
        lolRiotIdLabel.setText(a.getRiotId());
        lolLevelLabel.setText(String.valueOf(a.getSummonerLevel()));
        lolRankLabel.setText(a.getFormattedRank());
        lolRankLabel.setStyle("-fx-text-fill:" + color + ";-fx-font-size:13px;-fx-font-weight:bold;");
        tierBadge.setStyle("-fx-background-color:rgba(255,215,0,0.08);-fx-background-radius:40;" +
                "-fx-border-color:" + color + ";-fx-border-radius:40;-fx-border-width:2;" +
                "-fx-effect:dropshadow(gaussian," + color + "66,14,0.4,0,0);");
        tierEmojiLabel.setText(tierEmoji(a.getTier()));
        lolLpLabel.setText(a.getTier() == null || a.getTier().isBlank()
                ? "Unranked" : a.getLeaguePoints() + " LP");
        lolWlLabel.setText(a.getWins() + "W  " + a.getLosses() + "L");
        lolWrLabel.setText(riotService.getWinRate(a.getWins(), a.getLosses()));
    }

    private void populateSidebarStats(RiotAccount a) {
        // Rank badge
        sidebarRankBadge.setText("★  " + a.getFormattedRank().toUpperCase());
        sidebarRankPane.setVisible(true); sidebarRankPane.setManaged(true);

        // Quick stats box
        rankLabel.setText(a.getFormattedRank());
        winRateLabel.setText(riotService.getWinRate(a.getWins(), a.getLosses()));
        matchesLabel.setText(String.valueOf(a.getWins() + a.getLosses()));
        hoursLabel.setText("Lv. " + a.getSummonerLevel());
        quickStatsBox.setVisible(true); quickStatsBox.setManaged(true);
    }

    private void populateStatsGrid(RiotAccount a) {
        kdLabel.setText(riotService.getWinRate(a.getWins(), a.getLosses()));
        hsLabel.setText(String.valueOf(a.getWins() + a.getLosses()));
        mvpLabel.setText(a.getFormattedRank());
        streakLabel.setText(a.getLeaguePoints() + " LP");
        statsGrid.setVisible(true); statsGrid.setManaged(true);
    }

    private void showLolNoAccount() {
        lolNoAccount.setVisible(true);  lolNoAccount.setManaged(true);
        lolStats.setVisible(false);     lolStats.setManaged(false);
        refreshRiotBtn.setVisible(false); refreshRiotBtn.setManaged(false);
        // Hide sidebar LoL sections
        sidebarRankPane.setVisible(false); sidebarRankPane.setManaged(false);
        quickStatsBox.setVisible(false);   quickStatsBox.setManaged(false);
        statsGrid.setVisible(false);       statsGrid.setManaged(false);
        // Show no-matches message
        matchLoadingLabel.setText("Link your League account to see match history.");
        matchLoadingLabel.setVisible(true); matchLoadingLabel.setManaged(true);
    }

    // ── Match history ─────────────────────────────────────────────────────────

    private void loadMatchHistory(String puuid) {
        matchLoadingLabel.setText("⏳  Loading match history…");
        matchLoadingLabel.setVisible(true); matchLoadingLabel.setManaged(true);

        Task<List<MatchSummary>> task = new Task<>() {
            @Override protected List<MatchSummary> call() throws Exception {
                return riotService.fetchRecentMatches(puuid, 8);
            }
        };
        task.setOnSucceeded(e -> {
            List<MatchSummary> matches = task.getValue();
            matchList.getChildren().clear();
            if (matches.isEmpty()) {
                Label empty = new Label("No recent matches found.");
                empty.setStyle("-fx-text-fill:#3d4a5c;-fx-font-size:12px;-fx-padding:14 22;");
                matchList.getChildren().add(empty);
            } else {
                for (int i = 0; i < matches.size(); i++) {
                    matchList.getChildren().add(buildMatchRow(matches.get(i), i == matches.size() - 1));
                }
            }
        });
        task.setOnFailed(e -> {
            matchLoadingLabel.setText("⚠  Could not load match history.");
            matchLoadingLabel.setVisible(true); matchLoadingLabel.setManaged(true);
        });
        Thread t = new Thread(task, "match-history");
        t.setDaemon(true);
        t.start();
    }

    private HBox buildMatchRow(MatchSummary m, boolean last) {
        boolean win     = m.isWin();
        String  result  = win ? "WIN" : "LOSS";
        String  bg      = win ? "rgba(34,197,94,0.15)" : "rgba(248,113,113,0.12)";
        String  border  = win ? "#22c55e" : "#f87171";
        String  txtClr  = win ? "#22c55e" : "#f87171";
        String  rowBdr  = last ? "" : "-fx-border-color:#111c2e;-fx-border-width:0 0 1 0;";

        HBox row = new HBox(16);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:11 22;" + rowBdr);

        // WIN / LOSS badge
        StackPane badge = new StackPane();
        badge.setPrefSize(46, 22); badge.setMaxSize(46, 22);
        badge.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:4;" +
                "-fx-border-color:" + border + ";-fx-border-radius:4;-fx-border-width:1;");
        Label resultLbl = new Label(result);
        resultLbl.setStyle("-fx-text-fill:" + txtClr + ";-fx-font-size:10px;-fx-font-weight:bold;");
        badge.getChildren().add(resultLbl);

        // Champion + queue
        Label game = new Label(m.getChampionName() + "  —  " + m.getQueueName());
        game.setStyle("-fx-text-fill:#dde4f0;-fx-font-size:13px;");
        HBox.setHgrow(game, javafx.scene.layout.Priority.ALWAYS);

        // KDA
        Label kda = new Label(m.getKda());
        kda.setStyle("-fx-text-fill:#7a8899;-fx-font-size:12px;");

        // Duration
        Label dur = new Label(m.getDuration());
        dur.setStyle("-fx-text-fill:#5a6478;-fx-font-size:11px;");

        // Relative time
        Label time = new Label(m.getRelativeTime());
        time.setStyle("-fx-text-fill:#3d4a5c;-fx-font-size:11px;");

        row.getChildren().addAll(badge, game, kda, dur, time);
        return row;
    }

    // ── Refresh stats ─────────────────────────────────────────────────────────

    @FXML
    private void handleRefreshRiot(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;
        refreshRiotBtn.setDisable(true);
        refreshRiotBtn.setText("⏳  Refreshing…");
        showLolStatus("Fetching live stats…", "#00E5FF");

        Task<RiotAccount> task = new Task<>() {
            @Override protected RiotAccount call() throws Exception {
                return riotService.refreshAccount(user.getId());
            }
        };
        task.setOnSucceeded(e -> {
            refreshRiotBtn.setDisable(false);
            refreshRiotBtn.setText("↻  Refresh Stats");
            RiotAccount fresh = task.getValue();
            populateLolCard(fresh);
            populateSidebarStats(fresh);
            populateStatsGrid(fresh);
            loadMatchHistory(fresh.getPuuid());
            showLolStatus("✅  Stats refreshed.", "#22c55e");
        });
        task.setOnFailed(e -> {
            refreshRiotBtn.setDisable(false);
            refreshRiotBtn.setText("↻  Refresh Stats");
            Throwable cause = task.getException();
            showLolStatus("⚠  " + (cause != null ? cause.getMessage() : "Refresh failed."), "#ff4d4d");
        });
        new Thread(task, "riot-refresh").start();
    }

    private void showLolStatus(String text, String color) {
        lolStatusLabel.setText(text);
        lolStatusLabel.setStyle("-fx-text-fill:" + color + ";-fx-font-size:11px;");
        lolStatusLabel.setVisible(true); lolStatusLabel.setManaged(true);
    }

    // ── Favourite games ───────────────────────────────────────────────────────

    private void loadFavouriteGames(long userId) {
        List<UserGame> favs = gameService.getFavorites(userId);
        favGamesRow.getChildren().clear();
        if (favs.isEmpty()) {
            noFavLabel.setVisible(true); noFavLabel.setManaged(true);
            return;
        }
        noFavLabel.setVisible(false); noFavLabel.setManaged(false);
        for (UserGame g : favs) favGamesRow.getChildren().add(buildFavCard(g));
    }

    private VBox buildFavCard(UserGame g) {
        VBox card = new VBox(6);
        card.setPrefWidth(110); card.setMaxWidth(110);
        card.setStyle("-fx-background-color:rgba(10,22,40,0.90);-fx-background-radius:8;" +
                "-fx-border-color:#1e2d45;-fx-border-radius:8;-fx-border-width:1;-fx-padding:0 0 8 0;");
        StackPane cover = new StackPane();
        cover.setPrefSize(110, 70); cover.setMaxSize(110, 70);
        cover.setStyle("-fx-background-color:#0a1628;-fx-background-radius:8 8 0 0;");
        Label ph = new Label("🎮");
        ph.setStyle("-fx-font-size:22px;-fx-text-fill:#1e2d45;");
        cover.getChildren().add(ph);
        if (g.getCoverUrl() != null && !g.getCoverUrl().isBlank()) {
            Task<Image> t = new Task<>() {
                @Override protected Image call() {
                    return new Image(g.getCoverUrl(), 110, 70, false, true, false);
                }
            };
            t.setOnSucceeded(e -> {
                Image img = t.getValue();
                if (img != null && !img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(110); iv.setFitHeight(70); iv.setPreserveRatio(false);
                    Rectangle clip = new Rectangle(110, 70);
                    clip.setArcWidth(8); clip.setArcHeight(8); iv.setClip(clip);
                    Platform.runLater(() -> cover.getChildren().setAll(iv));
                }
            });
            Thread th = new Thread(t); th.setDaemon(true); th.start();
        }
        Label name = new Label(g.getGameName());
        name.setWrapText(true); name.setMaxWidth(94);
        name.setStyle("-fx-text-fill:#dde4f0;-fx-font-size:10px;-fx-font-weight:bold;-fx-padding:0 8;");
        Label genre = new Label(g.getGenre() != null ? g.getGenre() : "");
        genre.setStyle("-fx-text-fill:#5a6478;-fx-font-size:9px;-fx-padding:0 8;");
        card.getChildren().addAll(cover, name, genre);
        return card;
    }

    // ── Face ID ───────────────────────────────────────────────────────────────

    @FXML
    private void handleEnrollFace(ActionEvent event) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;
        enrollFaceButton.setDisable(true);
        enrollFaceButton.setText("📷  Enrolling…");
        Thread t = new Thread(() -> {
            try {
                faceClient.enroll(user.getId(), user.getEmail());
                Platform.runLater(() -> {
                    enrollFaceButton.setDisable(false);
                    enrollFaceButton.setText("🔁  Re-enroll Face ID");
                    Alert a = new Alert(Alert.AlertType.INFORMATION);
                    a.setTitle("Face ID Enrolled"); a.setHeaderText(null);
                    a.setContentText("Your face has been enrolled successfully!");
                    a.showAndWait();
                });
            } catch (FaceAuthClient.FaceAuthException e) {
                Platform.runLater(() -> {
                    enrollFaceButton.setDisable(false);
                    enrollFaceButton.setText("📷  Enroll Face ID");
                    Alert a = new Alert(Alert.AlertType.ERROR);
                    a.setTitle("Enrollment Failed"); a.setHeaderText("Could not enroll Face ID");
                    a.setContentText(e.getMessage()); a.showAndWait();
                });
            }
        }, "face-enroll");
        t.setDaemon(true); t.start();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void handleEditProfile(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/demo/view/profile-edit.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("TeamHub – Edit Profile");
        stage.setScene(new Scene(root, 1000, 860));
        stage.show();
    }

    @FXML
    private void handleManageGames(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/demo/view/game-library.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("TeamHub – Game Library");
        stage.setScene(new Scene(root, 1200, 750));
        stage.show();
    }

    @FXML
    private void handleLogout(ActionEvent event) throws IOException {
        SessionManager.clear();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/demo/view/login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("TeamHub – Login");
        stage.setScene(new Scene(root, 1000, 700));
        stage.show();
    }

    // ── Avatar (shared with ProfileEditController) ────────────────────────────

    static void applyAvatarToImageView(String avatarUrl, String initials,
                                        Label avatarLabel, ImageView avatarImageView) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            showInitials(avatarLabel, avatarImageView, initials); return;
        }
        try {
            Image img;
            if (avatarUrl.startsWith("builtin:")) {
                String fn = avatarUrl.substring("builtin:".length());
                InputStream is = ProfileController.class.getResourceAsStream(
                        "/tn/esprit/demo/view/avatars/" + fn);
                if (is == null) throw new IllegalArgumentException("Missing: " + fn);
                img = new Image(is);
            } else {
                img = new Image(avatarUrl, true);
            }
            double size = avatarImageView.getFitWidth() > 0 ? avatarImageView.getFitWidth() : 100;
            avatarImageView.setFitWidth(size); avatarImageView.setFitHeight(size);
            avatarImageView.setPreserveRatio(false);
            double r = size / 2.0;
            avatarImageView.setClip(new Circle(r, r, r));
            avatarImageView.setImage(img);
            avatarImageView.setVisible(true);  avatarImageView.setManaged(true);
            avatarLabel.setVisible(false);     avatarLabel.setManaged(false);
        } catch (Exception e) {
            showInitials(avatarLabel, avatarImageView, initials);
        }
    }

    private void applyAvatar(String url, String initials) {
        applyAvatarToImageView(url, initials, avatarLabel, avatarImageView);
    }

    private static void showInitials(Label lbl, ImageView iv, String initials) {
        lbl.setText(initials);
        lbl.setVisible(true);  lbl.setManaged(true);
        iv.setVisible(false);  iv.setManaged(false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String tierEmoji(String tier) {
        if (tier == null) return "—";
        return switch (tier.toUpperCase()) {
            case "IRON"        -> "🩶";
            case "BRONZE"      -> "🥉";
            case "SILVER"      -> "🥈";
            case "GOLD"        -> "🥇";
            case "PLATINUM"    -> "💎";
            case "EMERALD"     -> "💚";
            case "DIAMOND"     -> "🔷";
            case "MASTER"      -> "👑";
            case "GRANDMASTER" -> "🔥";
            case "CHALLENGER"  -> "⚡";
            default            -> "🏅";
        };
    }
}
