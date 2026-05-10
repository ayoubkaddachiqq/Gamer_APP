package tn.esprit.Controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.api.GameTrendingClient;
import tn.esprit.entities.Post;
import tn.esprit.services.ServicePost;

import java.io.IOException;
import java.util.List;

public class MyPostsController {

    @FXML
    private VBox myPostsContainer;

    @FXML
    private Label emptyLabel;

    private ServicePost servicePost = new ServicePost();
    private GameTrendingClient gameTrendingClient = new GameTrendingClient();
    private Stage primaryStage;
    private static final int CURRENT_USER_ID = 1;

    public void init(Stage primaryStage) {
        this.primaryStage = primaryStage;
        loadPosts();
    }

    public void loadPosts() {
        myPostsContainer.getChildren().clear();

        List<Post> posts = servicePost.getAll();
        posts.removeIf(p -> p.getUserId() != CURRENT_USER_ID);
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
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Edit Post");

        TextArea contentArea = new TextArea(post.getContent());
        contentArea.setPrefHeight(100);
        contentArea.setWrapText(true);
        contentArea.setStyle("-fx-control-inner-background: #0d1c2d; -fx-text-fill: white; -fx-background-radius: 10;");

        TextField gameTagField = new TextField();
        gameTagField.setText(post.getGameTag() != null ? post.getGameTag() : "General");
        gameTagField.setStyle("-fx-background-color: #0d1c2d; -fx-text-fill: white; -fx-prompt-text-fill: #64748b; -fx-background-radius: 8;");
        setupGameTagAutocomplete(gameTagField);

        Button saveBtn = new Button("Save Changes");
        saveBtn.setStyle("-fx-background-color: #00ff88; -fx-text-fill: #0d1c2d; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 30 8 30;");
        saveBtn.setOnAction(e -> {
            post.setContent(contentArea.getText());
            post.setGameTag(gameTagField.getText());
            servicePost.update(post);

            dialog.close();
            loadPosts();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #2c3a4c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 30 8 30;");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox buttonBox = new HBox(15, cancelBtn, saveBtn);
        buttonBox.setAlignment(Pos.CENTER);

        VBox dialogVBox = new VBox(15);
        dialogVBox.setPadding(new Insets(25));
        dialogVBox.setStyle("-fx-background-color: #1e293b;");
        dialogVBox.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Edit Post");
        titleLabel.setStyle("-fx-text-fill: #bd00ff; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label contentLabel = new Label("Content:");
        contentLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Label gameLabel = new Label("Game Tag:");
        gameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        dialogVBox.getChildren().addAll(titleLabel, contentLabel, contentArea, gameLabel, gameTagField, buttonBox);

        Scene scene = new Scene(dialogVBox, 500, 380);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void setupGameTagAutocomplete(TextField field) {
        ContextMenu suggestions = new ContextMenu();
        PauseTransition debounce = new PauseTransition(Duration.millis(300));

        field.textProperty().addListener((obs, oldVal, newVal) -> {
            debounce.setOnFinished(e -> {
                String query = newVal != null ? newVal.trim() : "";
                if (query.isEmpty()) {
                    suggestions.hide();
                    return;
                }
                new Thread(() -> {
                    List<String> results = gameTrendingClient.searchGames(query, 6);
                    Platform.runLater(() -> {
                        suggestions.getItems().clear();
                        if (results.isEmpty()) {
                            suggestions.hide();
                            return;
                        }
                        for (String name : results) {
                            MenuItem item = new MenuItem(name);
                            item.setOnAction(ev -> {
                                field.setText(name);
                                field.positionCaret(name.length());
                                suggestions.hide();
                            });
                            suggestions.getItems().add(item);
                        }
                        suggestions.show(field, Side.BOTTOM, 0, 0);
                    });
                }).start();
            });
            debounce.playFromStart();
        });

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                suggestions.hide();
            }
        });
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainInterface.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) myPostsContainer.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setTitle("Team Hub - E-Sport Recruitment");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
