package tn.esprit.Controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.api.GameTrendingClient;
import tn.esprit.entities.ImagePost;
import tn.esprit.utils.SessionManager;
import tn.esprit.entities.Post;
import tn.esprit.services.ServiceImagePost;
import tn.esprit.services.ServicePost;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MyPostsController {

    @FXML
    private VBox myPostsContainer;

    @FXML
    private Label emptyLabel;

    private ServicePost servicePost = new ServicePost();
    private ServiceImagePost serviceImagePost = new ServiceImagePost();
    private GameTrendingClient gameTrendingClient = new GameTrendingClient();
    private Stage primaryStage;

    public void init(Stage primaryStage) {
        this.primaryStage = primaryStage;
        loadPosts();
    }

    public void loadPosts() {
        myPostsContainer.getChildren().clear();

        List<Post> posts = servicePost.getAll();
        posts.removeIf(p -> p.getUserId() != SessionManager.getCurrentUser().getId());
        servicePost.loadImagesForPosts(posts);

        if (posts.isEmpty()) {
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
            return;
        }

        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        for (Post post : posts) {
            addPostCard(post);
        }
    }

    private void addPostCard(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PostCard.fxml"));
            VBox card = loader.load();
            PostCardController cardController = loader.getController();

            cardController.setData(post);
            cardController.setEditable(true);

            cardController.setOnEdit(p -> handleEdit(p, card));
            cardController.setOnDelete(() -> {
                servicePost.delete(post.getId());
                myPostsContainer.getChildren().remove(card);
                if (myPostsContainer.getChildren().isEmpty() || (myPostsContainer.getChildren().size() == 1 && myPostsContainer.getChildren().get(0) == emptyLabel)) {
                    emptyLabel.setVisible(true);
                    emptyLabel.setManaged(true);
                }
            });

            myPostsContainer.getChildren().add(card);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleEdit(Post post, VBox card) {
        boolean wasMaximized = primaryStage.isMaximized();

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Edit Post");
        dialog.setResizable(false);

        TextArea contentArea = new TextArea(post.getContent());
        contentArea.setPrefHeight(100);
        contentArea.setWrapText(true);
        contentArea.setStyle("-fx-control-inner-background: #0d1c2d; -fx-text-fill: white; -fx-background-radius: 10;");

        TextField gameTagField = new TextField();
        gameTagField.setText(post.getGameTag() != null ? post.getGameTag() : "General");
        gameTagField.setStyle("-fx-background-color: #0d1c2d; -fx-text-fill: white; -fx-prompt-text-fill: #64748b; -fx-background-radius: 8;");

        ListView<String> suggestionList = new ListView<>();
        suggestionList.setVisible(false);
        suggestionList.setManaged(false);
        suggestionList.setPrefHeight(0);
        suggestionList.setMaxHeight(120);
        suggestionList.setStyle("-fx-background-color: #0d1c2d; -fx-control-inner-background: #0d1c2d; -fx-text-fill: white;");

        PauseTransition debounce = new PauseTransition(Duration.millis(300));
        gameTagField.textProperty().addListener((obs, oldVal, newVal) -> {
            debounce.setOnFinished(e -> {
                String query = newVal != null ? newVal.trim() : "";
                if (query.isEmpty()) {
                    suggestionList.setVisible(false);
                    suggestionList.setManaged(false);
                    return;
                }
                new Thread(() -> {
                    List<String> results = gameTrendingClient.searchGames(query, 6);
                    Platform.runLater(() -> {
                        suggestionList.getItems().setAll(results);
                        if (results.isEmpty()) {
                            suggestionList.setVisible(false);
                            suggestionList.setManaged(false);
                        } else {
                            suggestionList.setVisible(true);
                            suggestionList.setManaged(true);
                            suggestionList.setPrefHeight(Math.min(results.size() * 28 + 10, 120));
                        }
                    });
                }).start();
            });
            debounce.playFromStart();
        });

        suggestionList.setOnMouseClicked(e -> {
            String selected = suggestionList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                gameTagField.setText(selected);
                suggestionList.setVisible(false);
                suggestionList.setManaged(false);
            }
        });

        List<ImagePost> existingImagePosts = serviceImagePost.getImagesByPost(post.getId());
        List<ImagePost> imagesToKeep = new ArrayList<>(existingImagePosts);
        List<File> newImageFiles = new ArrayList<>();

        VBox mediaPreviewBox = new VBox(8);

        Button addMediaBtn = new Button("+ Add Media");
        addMediaBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-cursor: hand;");
        addMediaBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
            fc.setInitialDirectory(new File(System.getProperty("user.home")));
            File selected = fc.showOpenDialog(dialog);
            if (selected != null) {
                newImageFiles.add(selected);
                refreshMediaPreview(mediaPreviewBox, imagesToKeep, newImageFiles);
            }
        });

        refreshMediaPreview(mediaPreviewBox, imagesToKeep, newImageFiles);

        Button saveBtn = new Button("Save Changes");
        saveBtn.setStyle("-fx-background-color: #00ff88; -fx-text-fill: #0d1c2d; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 30 8 30;");
        saveBtn.setOnAction(e -> {
            post.setContent(contentArea.getText());
            post.setGameTag(gameTagField.getText());
            servicePost.update(post);

            for (ImagePost img : existingImagePosts) {
                if (!imagesToKeep.contains(img)) {
                    try { Files.deleteIfExists(Paths.get(img.getImagePath())); } catch (IOException ignored) {}
                    serviceImagePost.removeImage(img.getId());
                }
            }

            for (File f : newImageFiles) {
                try {
                    String fileName = "post_" + post.getId() + "_" + System.currentTimeMillis() + "_" + f.getName();
                    String destPath = "uploads/images/" + fileName;
                    Path dest = Paths.get(destPath).toAbsolutePath();
                    Files.copy(f.toPath(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    serviceImagePost.addImage(post.getId(), destPath);
                } catch (IOException ex) {
                    System.err.println("Error saving image: " + ex.getMessage());
                }
            }

            dialog.close();
            loadPosts();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2c3a4c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 30 8 30;");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox buttonBox = new HBox(15, cancelBtn, saveBtn);
        buttonBox.setAlignment(Pos.CENTER);

        VBox dialogVBox = new VBox(12);
        dialogVBox.setPadding(new Insets(25));
        dialogVBox.setStyle("-fx-background-color: #1e293b;");
        dialogVBox.setAlignment(Pos.TOP_CENTER);

        Label titleLabel = new Label("Edit Post");
        titleLabel.setStyle("-fx-text-fill: #bd00ff; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label contentLabel = new Label("Content:");
        contentLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Label gameLabel = new Label("Game Tag:");
        gameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Label mediaLabel = new Label("Media:");
        mediaLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        dialogVBox.getChildren().addAll(
                titleLabel,
                contentLabel, contentArea,
                gameLabel, gameTagField, suggestionList,
                mediaLabel, addMediaBtn, mediaPreviewBox,
                buttonBox
        );

        Scene scene = new Scene(dialogVBox, 500, 520);
        String css = getClass().getResource("/style.css").toExternalForm();
        if (css != null) scene.getStylesheets().add(css);
        dialog.setScene(scene);
        dialog.showAndWait();

        if (wasMaximized) {
            primaryStage.setMaximized(true);
        }
    }

    private void refreshMediaPreview(VBox container, List<ImagePost> imagesToKeep, List<File> newImageFiles) {
        container.getChildren().clear();
        HBox row = new HBox(10);
        row.setStyle("-fx-padding: 5 0; -fx-wrap-space: true;");

        for (ImagePost img : imagesToKeep) {
            VBox thumbBox = buildThumbnail(new File(img.getImagePath()).toURI().toString(), () -> {
                imagesToKeep.remove(img);
                refreshMediaPreview(container, imagesToKeep, newImageFiles);
            });
            row.getChildren().add(thumbBox);
        }

        for (File f : newImageFiles) {
            VBox thumbBox = buildThumbnail(f.toURI().toString(), () -> {
                newImageFiles.remove(f);
                refreshMediaPreview(container, imagesToKeep, newImageFiles);
            });
            row.getChildren().add(thumbBox);
        }

        if (!row.getChildren().isEmpty()) {
            container.getChildren().add(row);
        }
    }

    private VBox buildThumbnail(String imageUri, Runnable onRemove) {
        VBox thumbBox = new VBox(5);
        thumbBox.setAlignment(Pos.CENTER);

        ImageView imageView = new ImageView();
        try {
            Image image = new Image(imageUri, 80, 80, true, true);
            imageView.setImage(image);
        } catch (Exception e) {
            imageView.setFitWidth(80);
            imageView.setFitHeight(80);
        }
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.setStyle("-fx-background-radius: 8; -fx-border-color: rgba(0, 238, 252, 0.3); -fx-border-radius: 8;");

        Button removeBtn = new Button("x");
        removeBtn.setStyle("-fx-background-color: #ff4b4b; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 50%; -fx-min-width: 20px; -fx-min-height: 20px; -fx-cursor: hand;");
        removeBtn.setOnAction(e -> onRemove.run());

        thumbBox.getChildren().addAll(imageView, removeBtn);
        return thumbBox;
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainInterface.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) myPostsContainer.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setTitle("Team Hub - E-Sport Recruitment");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
