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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.entities.UserGame;
import tn.esprit.services.GameService;
import tn.esprit.utils.SessionManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class GameLibraryController {

    private final GameService gameService = new GameService();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML private VBox gamesContainer;
    @FXML private Label countLabel;

    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;
        loadGames();
    }

    private void loadGames() {
        gamesContainer.getChildren().clear();
        List<UserGame> games = gameService.getUserGames(currentUser.getId());
        countLabel.setText(String.valueOf(games.size()));

        for (UserGame game : games) {
            VBox card = new VBox(10);
            card.setStyle("-fx-background-color: #1e293b; -fx-padding: 15; -fx-background-radius: 10; -fx-border-color: rgba(255,255,255,0.1); -fx-border-radius: 10;");

            HBox header = new HBox(15);
            header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            if (game.getCoverUrl() != null && !game.getCoverUrl().isEmpty()) {
                ImageView cover = new ImageView();
                cover.setFitWidth(60);
                cover.setFitHeight(80);
                cover.setPreserveRatio(true);
                loadCoverImage(game.getCoverUrl(), cover);
                header.getChildren().add(cover);
            }

            VBox info = new VBox(5);
            Label nameLabel = new Label(game.getGameName());
            nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

            Label genreLabel = new Label(game.getGenre() != null ? game.getGenre() : "Unknown genre");
            genreLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

            VBox stats = new VBox(2);
            Label metaLabel = new Label("Metacritic: " + (game.getMetacriticScore() > 0 ? game.getMetacriticScore() : "N/A"));
            metaLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

            Label favLabel = new Label(game.isFavorite() ? "★ Favorite" : "☆ Not Favorite");
            favLabel.setStyle("-fx-text-fill: " + (game.isFavorite() ? "#ffd700" : "#64748b") + "; -fx-font-size: 11px;");

            stats.getChildren().addAll(metaLabel, favLabel);
            info.getChildren().addAll(nameLabel, genreLabel, stats);

            HBox actions = new HBox(10);
            actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            Button favBtn = new Button(game.isFavorite() ? "★ Unfavorite" : "☆ Favorite");
            favBtn.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: #4fc3f7; -fx-background-radius: 15; -fx-padding: 5 15; -fx-cursor: hand;");
            int gameId = game.getId();
            favBtn.setOnAction(e -> {
                gameService.toggleFavorite(gameId);
                loadGames();
            });

            Button removeBtn = new Button("✕ Remove");
            removeBtn.setStyle("-fx-background-color: #5f1e1e; -fx-text-fill: #ff4b4b; -fx-background-radius: 15; -fx-padding: 5 15; -fx-cursor: hand;");
            removeBtn.setOnAction(e -> {
                gameService.removeGame(gameId);
                loadGames();
            });

            actions.getChildren().addAll(favBtn, removeBtn);

            header.getChildren().add(info);
            card.getChildren().addAll(header, actions);
            gamesContainer.getChildren().add(card);
        }

        if (games.isEmpty()) {
            Label empty = new Label("No games in your library yet.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
            gamesContainer.getChildren().add(empty);
        }
    }

    private void loadCoverImage(String url, ImageView view) {
        new Thread(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() == 200) {
                    javafx.application.Platform.runLater(() ->
                        view.setImage(new Image(new ByteArrayInputStream(resp.body()))));
                }
            } catch (Exception e) {
                System.err.println("Failed to load cover: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void handleBack(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Profile.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 700));
        stage.setTitle("Team Hub - Profile");
        stage.setMaximized(true);
        stage.show();
    }
}
