package tn.esprit.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.UserRanking;
import tn.esprit.entities.ImagePost;
import tn.esprit.entities.Post;
import tn.esprit.services.ServiceImagePost;
import tn.esprit.services.ServicePost;
import tn.esprit.entities.Share;
import tn.esprit.services.ServiceShare;

import javafx.application.Platform;
import tn.esprit.api.GameTrendingClient;
import tn.esprit.api.TextFixClient;
import tn.esprit.api.ToxicityClient;
import tn.esprit.api.model.TextFixResult;
import tn.esprit.api.model.ToxicityResult;
import tn.esprit.api.model.TrendingGame;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML
    private VBox feedContainer;

    @FXML
    private TextArea postInput;

    @FXML
    private ComboBox<String> gameTagSelector;

    @FXML
    private TextField searchBar;

    @FXML
    private HBox filterBar;

    @FXML
    private ComboBox<String> filterTime;

    @FXML
    private ComboBox<String> filterGameTag;

    @FXML
    private ComboBox<String> filterType;

    @FXML
    private VBox mediaPreviewContainer;

    @FXML
    private ImageView headerAvatarImage;

    @FXML
    private StackPane headerAvatarStack;

    @FXML
    private Label headerUsernameLabel;

    @FXML
    private VBox leaderboardList;

    @FXML
    private VBox trendingGamesContainer;

    private ServicePost servicePost = new ServicePost();
    private GameTrendingClient gameTrendingClient = new GameTrendingClient();
    private TextFixClient textFixClient = new TextFixClient();
    private ToxicityClient toxicityClient = new ToxicityClient();
    private ServiceShare serviceShare = new ServiceShare();
    private ServiceImagePost serviceImagePost = new ServiceImagePost();
    private static final int CURRENT_USER_ID = 1;
    private static final String DEFAULT_AVATAR = "uploads/profiles/default.png";

    private List<File> tempSelectedImages = new ArrayList<>();

    private String getProfilePhoto(int userId) {
        try (java.sql.PreparedStatement ps = tn.esprit.utils.MyDB.getInstance().getConnection().prepareStatement("SELECT profile_photo FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String path = rs.getString("profile_photo");
                if (path != null && !path.isEmpty()) return path;
            }
        } catch (Exception e) { /* ignore */ }
        return DEFAULT_AVATAR;
    }

    private String getUsername(int userId) {
        try (java.sql.PreparedStatement ps = tn.esprit.utils.MyDB.getInstance().getConnection().prepareStatement("SELECT username FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("username");
        } catch (Exception e) { /* ignore */ }
        return "Player " + userId;
    }

    private String[] gameTags = {
        "General", "VALORANT", "League of Legends", "CS2", "Fortnite",
        "Apex Legends", "Overwatch 2", "Dota 2", "Rocket League",
        "EA FC 25", "Call of Duty", "Minecraft", "GTA V", "Rainbow Six Siege"
    };

    @FXML
    public void initialize() {
        System.out.println("Initializing controller...");
        gameTagSelector.getItems().addAll(gameTags);
        gameTagSelector.setValue("General");

        filterTime.getItems().addAll("All Time", "Last Hour", "Today", "This Week");
        filterTime.setValue("All Time");

        filterGameTag.getItems().addAll("All", "VALORANT", "League of Legends", "CS2", "Fortnite",
            "Apex Legends", "Overwatch 2", "Dota 2", "Rocket League",
            "EA FC 25", "Call of Duty", "Minecraft", "GTA V", "Rainbow Six Siege", "General");
        filterGameTag.setValue("All");

        filterType.getItems().addAll("All", "Content", "Users");
        filterType.setValue("All");

        loadHeaderProfile();
        setupSearchBar();
        loadFeed();
        loadTrendingGames();
    }

    private void loadHeaderProfile() {
        if (headerUsernameLabel != null) {
            headerUsernameLabel.setText(getUsername(CURRENT_USER_ID));
        }

        if (headerAvatarImage != null) {
            String photoPath = getProfilePhoto(CURRENT_USER_ID);
            File imgFile = new File(photoPath).getAbsoluteFile();
            System.out.println("Loading header avatar from: " + imgFile.getAbsolutePath() + " (exists: " + imgFile.exists() + ")");
            if (imgFile.exists()) {
                Image image = new Image(imgFile.toURI().toString());
                double size = 36;
                double scale = Math.max(size / image.getWidth(), size / image.getHeight());
                headerAvatarImage.setFitWidth(image.getWidth() * scale);
                headerAvatarImage.setFitHeight(image.getHeight() * scale);
                headerAvatarImage.setImage(image);
                headerAvatarImage.setPreserveRatio(true);
                headerAvatarImage.setClip(new Circle(size / 2));
            }
        }
    }

    @FXML
    private void handleChangeProfilePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        File selectedFile = fileChooser.showOpenDialog(headerAvatarImage.getScene().getWindow());
        if (selectedFile != null) {
            try {
                String fileName = "user_" + CURRENT_USER_ID + "_" + System.currentTimeMillis() + "_" + selectedFile.getName();
                String destPath = "uploads/profiles/" + fileName;
                Path dest = Paths.get(destPath).toAbsolutePath();
                Files.copy(selectedFile.toPath(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Saved profile photo to: " + dest.toAbsolutePath());
                try (java.sql.PreparedStatement ps = tn.esprit.utils.MyDB.getInstance().getConnection().prepareStatement("UPDATE users SET profile_photo = ? WHERE id = ?")) {
                    ps.setString(1, destPath);
                    ps.setInt(2, CURRENT_USER_ID);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                loadHeaderProfile();
            } catch (IOException e) {
                System.err.println("Error saving profile photo: " + e.getMessage());
            }
        }
    }

    private void setupSearchBar() {
        if (searchBar != null) {
            searchBar.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    filterBar.setVisible(true);
                    filterBar.setManaged(true);
                }
            });

            searchBar.setOnKeyPressed(e -> {
                switch (e.getCode()) {
                    case ENTER:
                        applyFilters();
                        break;
                    case ESCAPE:
                        clearAll();
                        break;
                }
            });
        }
    }

    private void loadFeed() {
        feedContainer.getChildren().clear();
        List<Post> posts = servicePost.getTrendingPosts();
        servicePost.loadImagesForPosts(posts);
        System.out.println("Fetched " + posts.size() + " posts sorted by trending score.");
        for (Post p : posts) {
            addPostToFeed(p);
        }
        loadLeaderboard();
    }

    private void loadLeaderboard() {
        if (leaderboardList == null) return;
        leaderboardList.getChildren().clear();
        List<UserRanking> rankings = servicePost.getUserLeaderboard();
        int rank = 1;
        for (UserRanking u : rankings) {
            HBox row = new HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.getStyleClass().add("leaderboard-row");

            Label rankLabel = new Label("#" + rank);
            rankLabel.setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold; -fx-font-size: 13px; -fx-min-width: 25px;");
            if (rank == 1) rankLabel.setStyle("-fx-text-fill: #ffd700; -fx-font-weight: bold; -fx-font-size: 15px; -fx-min-width: 25px;");
            else if (rank == 2) rankLabel.setStyle("-fx-text-fill: #c0c0c0; -fx-font-weight: bold; -fx-font-size: 14px; -fx-min-width: 25px;");
            else if (rank == 3) rankLabel.setStyle("-fx-text-fill: #cd7f32; -fx-font-weight: bold; -fx-font-size: 14px; -fx-min-width: 25px;");

            VBox infoBox = new VBox(2);
            HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

            Label nameLabel = new Label(u.getUsername());
            nameLabel.getStyleClass().add("leaderboard-name");
            Label statsLabel = new Label(u.getTitle() + " | Score: " + u.getScore());
            statsLabel.getStyleClass().add("leaderboard-stats");

            Label badgeLabel = new Label(getBadgeEmoji(u.getTitle()));
            badgeLabel.setStyle("-fx-font-size: 16px;");

            infoBox.getChildren().addAll(nameLabel, statsLabel);
            row.getChildren().addAll(rankLabel, badgeLabel, infoBox);
            leaderboardList.getChildren().add(row);
            rank++;
        }
    }

    private void loadTrendingGames() {
        if (trendingGamesContainer == null) return;
        new Thread(() -> {
            try {
                List<TrendingGame> games = gameTrendingClient.fetch();

                List<byte[]> imageBytesList = new ArrayList<>();
                for (TrendingGame game : games) {
                    if (game.getImageUrl() != null && !game.getImageUrl().isEmpty()) {
                        try {
                            imageBytesList.add(gameTrendingClient.downloadImage(game.getImageUrl()));
                        } catch (Exception e) {
                            imageBytesList.add(null);
                        }
                    } else {
                        imageBytesList.add(null);
                    }
                }

                Platform.runLater(() -> {
                    trendingGamesContainer.getChildren().clear();
                    for (int i = 0; i < games.size(); i++) {
                        TrendingGame game = games.get(i);
                        byte[] imageBytes = imageBytesList.get(i);

                        HBox row = new HBox(10);
                        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        row.getStyleClass().add("trending-item");

                        Label rankLabel = new Label(String.valueOf(i + 1));
                        rankLabel.setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold; -fx-min-width: 18px;");

                        ImageView thumbView = new ImageView();
                        thumbView.setFitWidth(36);
                        thumbView.setFitHeight(36);
                        thumbView.setPreserveRatio(true);
                        if (imageBytes != null) {
                            thumbView.setImage(new Image(new java.io.ByteArrayInputStream(imageBytes)));
                        }
                        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(36, 36);
                        clip.setArcWidth(8);
                        clip.setArcHeight(8);
                        thumbView.setClip(clip);

                        VBox infoBox = new VBox(2);
                        HBox.setHgrow(infoBox, javafx.scene.layout.Priority.ALWAYS);

                        Label nameLabel = new Label(game.getName());
                        nameLabel.getStyleClass().add("player-name");

                        Label ratingLabel = new Label("⭐ " + String.format("%.1f", game.getRating()));
                        ratingLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 9px;");

                        infoBox.getChildren().addAll(nameLabel, ratingLabel);
                        row.getChildren().addAll(rankLabel, thumbView, infoBox);
                        trendingGamesContainer.getChildren().add(row);
                    }
                });
            } catch (Exception e) {
                System.err.println("⚠  Error fetching trending games: " + e.getMessage());
                Platform.runLater(() -> {
                    trendingGamesContainer.getChildren().clear();
                    Label errorLabel = new Label("Failed to load");
                    errorLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                    trendingGamesContainer.getChildren().add(errorLabel);
                });
            }
        }).start();
    }

    private String getBadgeEmoji(String title) {
        switch (title) {
            case "Legend": return "👑";
            case "Elite": return "🔥";
            case "Veteran": return "⭐";
            default: return "🎮";
        }
    }

    @FXML
    private void applyFilters() {
        String query = searchBar.getText();
        String timeFilter = filterTime.getValue();
        String gameTagFilter = filterGameTag.getValue();
        String typeFilter = filterType.getValue();

        boolean hasSearch = query != null && !query.trim().isEmpty();
        boolean hasTimeFilter = timeFilter != null && !"All Time".equals(timeFilter);
        boolean hasGameTagFilter = gameTagFilter != null && !"All".equals(gameTagFilter);
        boolean hasTypeFilter = typeFilter != null && !"All".equals(typeFilter);

        if (!hasSearch && !hasTimeFilter && !hasGameTagFilter && !hasTypeFilter) {
            loadFeed();
            return;
        }

        String gameTag = hasGameTagFilter ? gameTagFilter : null;
        String time = hasTimeFilter ? timeFilter : null;
        String type = hasTypeFilter ? typeFilter : null;

        feedContainer.getChildren().clear();
        List<Post> results = servicePost.searchCombined(query, gameTag, time, type);
        servicePost.loadImagesForPosts(results);
        System.out.println("Combined search returned " + results.size() + " results (query='" + query + "', tag=" + gameTag + ", time=" + time + ", type=" + type + ")");
        for (Post p : results) {
            addPostToFeed(p);
        }
    }

    @FXML
    private void clearFilters() {
        searchBar.clear();
        filterTime.setValue("All Time");
        filterGameTag.setValue("All");
        filterType.setValue("All");
        filterBar.setVisible(false);
        filterBar.setManaged(false);
        loadFeed();
    }

    private void clearAll() {
        clearFilters();
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void handleCreatePost() {
        String content = postInput.getText();
        String selectedGame = gameTagSelector.getValue();

        if ((content == null || content.trim().isEmpty()) && tempSelectedImages.isEmpty()) return;

        String textToCheck = content != null ? content.trim() : "";
        if (!textToCheck.isEmpty() && toxicityClient.isConfigured()) {
            new Thread(() -> {
                ToxicityResult result = toxicityClient.check(textToCheck);
                if (result != null && result.isToxic()) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING,
                            "⚠ Content Flagged\n\nYour message was flagged as " + result.getLabel()
                            + " (" + String.format("%.0f", result.getScore() * 100) + "% confidence).\nPlease revise.",
                            ButtonType.OK);
                        alert.show();
                    });
                    return;
                }
                Platform.runLater(() -> createPost(content, selectedGame));
            }).start();
        } else {
            createPost(content, selectedGame);
        }
    }

    private void createPost(String content, String selectedGame) {
        Post newPost = new Post();
        newPost.setContent(content);
        newPost.setGameTag(selectedGame != null ? selectedGame : "General");
        newPost.setUserId(CURRENT_USER_ID);
        newPost.setUsername("Ayoub");

        int postId = servicePost.add(newPost);
        if (postId > 0) {
            newPost.setId(postId);
            if (!tempSelectedImages.isEmpty()) {
                for (File img : tempSelectedImages) {
                    try {
                        String fileName = "post_" + postId + "_" + System.currentTimeMillis() + "_" + img.getName();
                        String destPath = "uploads/images/" + fileName;
                        Path dest = Paths.get(destPath).toAbsolutePath();
                        Files.copy(img.toPath(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Saved post image to: " + dest.toAbsolutePath());
                        serviceImagePost.addImage(postId, destPath);
                        newPost.addImagePath(destPath);
                    } catch (IOException e) {
                        System.err.println("Error saving image: " + e.getMessage());
                    }
                }
            }
        }

        tempSelectedImages.clear();
        if (mediaPreviewContainer != null) {
            mediaPreviewContainer.getChildren().clear();
        }
        loadFeed();
        postInput.clear();
    }

    @FXML
    private void handleMedia() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            tempSelectedImages.addAll(selectedFiles);
            updateMediaPreview();
        }
    }

    private void updateMediaPreview() {
        if (mediaPreviewContainer == null) return;
        mediaPreviewContainer.getChildren().clear();

        if (tempSelectedImages.isEmpty()) return;

        HBox previewRow = new HBox(10);
        previewRow.setStyle("-fx-padding: 5 0;");

        for (File imgFile : tempSelectedImages) {
            VBox thumbBox = new VBox(5);
            thumbBox.setAlignment(javafx.geometry.Pos.CENTER);

            Image image = new Image(imgFile.toURI().toString(), 80, 80, true, true);
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(80);
            imageView.setFitHeight(80);
            imageView.setStyle("-fx-background-radius: 8; -fx-border-color: rgba(0, 238, 252, 0.3); -fx-border-radius: 8;");

            Label fileName = new Label(imgFile.getName());
            fileName.setStyle("-fx-text-fill: #64748b; -fx-font-size: 9px;");
            fileName.setMaxWidth(80);
            fileName.setWrapText(true);

            Button removeBtn = new Button("x");
            removeBtn.setStyle("-fx-background-color: #ff4b4b; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 50%; -fx-min-width: 20px; -fx-min-height: 20px; -fx-cursor: hand;");
            removeBtn.setOnAction(e -> {
                tempSelectedImages.remove(imgFile);
                updateMediaPreview();
            });

            thumbBox.getChildren().addAll(imageView, removeBtn);
            previewRow.getChildren().add(thumbBox);
        }

        mediaPreviewContainer.getChildren().add(previewRow);
    }

    private void addPostToFeed(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PostCard.fxml"));
            VBox card = loader.load();
            PostCardController cardController = loader.getController();
            cardController.setData(post);
            cardController.setOnShare(() -> handleSharePost(post));
            feedContainer.getChildren().add(card);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleSharePost(Post originalPost) {
        Share share = new Share(originalPost.getId(), CURRENT_USER_ID, "Check out this post!");
        serviceShare.addShare(share);

        String shareContent = "Shared from " +
            (originalPost.getUsername() != null ? originalPost.getUsername() : "Player " + originalPost.getUserId()) +
            ":\n" + originalPost.getContent();

        Post sharedPost = new Post();
        sharedPost.setContent(shareContent);
        sharedPost.setGameTag(originalPost.getGameTag());
        sharedPost.setUserId(CURRENT_USER_ID);
        sharedPost.setUsername("Ayoub");

        int sharedPostId = servicePost.add(sharedPost);

        List<ImagePost> originalImages = serviceImagePost.getImagesByPost(originalPost.getId());
        for (ImagePost img : originalImages) {
            serviceImagePost.addImage(sharedPostId, img.getImagePath());
        }

        loadFeed();
    }

    @FXML
    private void handleMyPosts() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MyPosts.fxml"));
            javafx.scene.Parent root = loader.load();
            tn.esprit.Controller.MyPostsController controller = loader.getController();
            controller.init((Stage) feedContainer.getScene().getWindow());
            Stage stage = (Stage) feedContainer.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setTitle("My Posts - Team Hub");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFixPost() {
        String content = postInput.getText();
        if (content == null || content.trim().isEmpty()) return;

        new Thread(() -> {
            TextFixResult result = textFixClient.fix(content);
            Platform.runLater(() -> {
                if (result.isSuccess()) {
                    postInput.setText(result.getFixedText());
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING, result.getError(), ButtonType.OK);
                    alert.show();
                }
            });
        }).start();
    }

    @FXML
    private void handleHome() {
        clearFilters();
    }

    @FXML
    private void handleExplore() {
        System.out.println("Explore clicked - feature coming soon");
    }

    @FXML
    private void handleTournaments() {
        System.out.println("Tournaments clicked - feature coming soon");
    }

    @FXML
    private void handleNotifications() {
        System.out.println("Notifications clicked - feature coming soon");
    }

    @FXML
    private void handleMessages() {
        System.out.println("Messages clicked - feature coming soon");
    }

    @FXML
    private void handleSettings() {
        System.out.println("Settings clicked - feature coming soon");
    }
}
