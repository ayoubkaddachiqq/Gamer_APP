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
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import tn.esprit.demo.model.UserGame;
import tn.esprit.demo.service.GameService;
import tn.esprit.demo.service.ServiceRegistry;
import tn.esprit.demo.service.SessionManager;

import java.io.IOException;
import java.util.List;

public class GameLibraryController {

    // ── FXML bindings ─────────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Button searchBtn;
    @FXML private ProgressIndicator searchSpinner;
    @FXML private Label searchStatusLabel;

    @FXML private VBox resultsSection;
    @FXML private FlowPane resultsPane;
    @FXML private Label resultCountLabel;

    @FXML private FlowPane libraryPane;
    @FXML private Label libraryCountLabel;
    @FXML private Label libraryEmptyLabel;
    @FXML private Label favLimitLabel;

    // ── State ─────────────────────────────────────────────────────────────────
    private final GameService gameService = ServiceRegistry.getGameService();
    private long userId;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final String CARD_BG =
            "-fx-background-color: rgba(12,18,34,0.92);" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #1a2d42;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;";
    private static final String CARD_HOVER =
            "-fx-background-color: rgba(18,25,46,0.97);" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #00E5FF;" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 1;";

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        userId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getId() : -1;

        searchField.setOnAction(e -> handleSearch());
        refreshLibrary();
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        setSearchState(true);
        hideSearchStatus();

        Task<List<UserGame>> task = new Task<>() {
            @Override
            protected List<UserGame> call() {
                return gameService.searchGames(query);
            }
        };

        task.setOnSucceeded(e -> {
            setSearchState(false);
            List<UserGame> results = task.getValue();
            populateResults(results);
        });

        task.setOnFailed(e -> {
            setSearchState(false);
            Throwable ex = task.getException();
            showSearchStatus(ex != null ? ex.getMessage()
                    : "Could not reach game database. Check your connection.", false);
        });

        daemon(task);
    }

    private void populateResults(List<UserGame> results) {
        resultsPane.getChildren().clear();

        if (results.isEmpty()) {
            showSearchStatus("No games found for your search.", false);
            resultsSection.setVisible(false);
            resultsSection.setManaged(false);
            return;
        }

        showSearchStatus("", true); // hide
        resultsSection.setVisible(true);
        resultsSection.setManaged(true);
        resultCountLabel.setText(results.size() + " result" + (results.size() == 1 ? "" : "s"));

        for (UserGame g : results) {
            boolean saved = userId >= 0 && gameService.isGameSaved(userId, g.getRawgId());
            resultsPane.getChildren().add(buildResultCard(g, saved));
        }
    }

    // ── Library ───────────────────────────────────────────────────────────────

    private void refreshLibrary() {
        libraryPane.getChildren().clear();
        hideFavLimit();

        if (userId < 0) return;

        List<UserGame> library = gameService.getUserLibrary(userId);
        libraryCountLabel.setText(library.size() + " game" + (library.size() == 1 ? "" : "s"));

        if (library.isEmpty()) {
            libraryEmptyLabel.setVisible(true);
            libraryEmptyLabel.setManaged(true);
            return;
        }
        libraryEmptyLabel.setVisible(false);
        libraryEmptyLabel.setManaged(false);

        for (UserGame g : library) {
            libraryPane.getChildren().add(buildLibraryCard(g));
        }
    }

    // ── Card builders ─────────────────────────────────────────────────────────

    /** 160 × 260 search-result card */
    private VBox buildResultCard(UserGame g, boolean alreadySaved) {
        VBox card = new VBox(0);
        card.setPrefWidth(160);
        card.setMaxWidth(160);
        card.setStyle(CARD_BG);
        card.setOnMouseEntered(e -> card.setStyle(CARD_HOVER));
        card.setOnMouseExited(e -> card.setStyle(CARD_BG));

        // Cover image
        StackPane coverPane = makeCoverPane(g.getCoverUrl(), 160, 100);

        // Info
        VBox info = new VBox(4);
        info.setStyle("-fx-padding: 8 10 10 10;");

        Label name = new Label(g.getGameName());
        name.setWrapText(true);
        name.setMaxWidth(140);
        name.setStyle("-fx-text-fill: #eef2f7; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label genre = new Label(g.getGenre() != null && !g.getGenre().isEmpty()
                ? g.getGenre() : "—");
        genre.setStyle("-fx-text-fill: #5a6478; -fx-font-size: 10px;");

        HBox metaRow = new HBox(4);
        metaRow.setStyle("-fx-alignment: CENTER_LEFT;");
        if (g.getMetacriticScore() > 0) {
            String mcStyle = metacriticColour(g.getMetacriticScore());
            Label mc = new Label("MC " + g.getMetacriticScore());
            mc.setStyle("-fx-text-fill: " + mcStyle + "; -fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-border-color: " + mcStyle + "; -fx-border-radius: 3;" +
                        "-fx-border-width: 1; -fx-padding: 1 4;");
            metaRow.getChildren().add(mc);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button addBtn = new Button(alreadySaved ? "✓ Saved" : "+ Add");
        addBtn.setPrefWidth(140);
        addBtn.setDisable(alreadySaved);
        addBtn.setStyle(alreadySaved ? savedBtnStyle() : addBtnStyle());
        addBtn.setOnAction(e -> {
            gameService.addGameToLibrary(userId, g);
            addBtn.setText("✓ Saved");
            addBtn.setDisable(true);
            addBtn.setStyle(savedBtnStyle());
            refreshLibrary();
        });

        info.getChildren().addAll(name, genre, metaRow, spacer, addBtn);
        card.getChildren().addAll(coverPane, info);
        return card;
    }

    /** 160 × 260 library card (with remove + star) */
    private VBox buildLibraryCard(UserGame g) {
        VBox card = new VBox(0);
        card.setPrefWidth(160);
        card.setMaxWidth(160);
        card.setStyle(CARD_BG);
        card.setOnMouseEntered(e -> card.setStyle(CARD_HOVER));
        card.setOnMouseExited(e -> card.setStyle(CARD_BG));

        StackPane coverPane = makeCoverPane(g.getCoverUrl(), 160, 100);

        VBox info = new VBox(4);
        info.setStyle("-fx-padding: 8 10 10 10;");

        Label name = new Label(g.getGameName());
        name.setWrapText(true);
        name.setMaxWidth(140);
        name.setStyle("-fx-text-fill: #eef2f7; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label genre = new Label(g.getGenre() != null && !g.getGenre().isEmpty()
                ? g.getGenre() : "—");
        genre.setStyle("-fx-text-fill: #5a6478; -fx-font-size: 10px;");

        // Favourite star toggle
        boolean fav = g.isFavorite();
        Button starBtn = new Button(fav ? "★ Fav" : "☆ Fav");
        starBtn.setStyle(fav ? favOnStyle() : favOffStyle());
        starBtn.setPrefWidth(140);
        starBtn.setOnAction(e -> {
            boolean nowFav = !g.isFavorite();
            try {
                gameService.toggleFavorite(userId, g.getRawgId(), nowFav);
                g.setFavorite(nowFav);
                starBtn.setText(nowFav ? "★ Fav" : "☆ Fav");
                starBtn.setStyle(nowFav ? favOnStyle() : favOffStyle());
                hideFavLimit();
                refreshLibrary(); // rebuild so profile card count also updates
            } catch (IllegalStateException ex) {
                showFavLimit();
            }
        });

        // Remove button
        Button removeBtn = new Button("Remove");
        removeBtn.setPrefWidth(140);
        removeBtn.setStyle(removeBtnStyle());
        removeBtn.setOnAction(e -> {
            gameService.removeGameFromLibrary(userId, g.getRawgId());
            refreshLibrary();
            // also re-render search results to re-enable add buttons
            refreshResultsAddButtons();
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        info.getChildren().addAll(name, genre, spacer, starBtn, removeBtn);
        card.getChildren().addAll(coverPane, info);
        return card;
    }

    // ── Cover image pane (async load) ─────────────────────────────────────────

    private StackPane makeCoverPane(String url, double w, double h) {
        StackPane pane = new StackPane();
        pane.setPrefSize(w, h);
        pane.setMaxSize(w, h);
        pane.setStyle("-fx-background-color: #0a1628;");

        // Placeholder icon
        Label placeholder = new Label("🎮");
        placeholder.setStyle("-fx-font-size: 28px; -fx-text-fill: #1e2d45;");
        pane.getChildren().add(placeholder);

        if (url != null && !url.isBlank()) {
            Task<Image> imgTask = new Task<>() {
                @Override
                protected Image call() {
                    return new Image(url, w, h, false, true, false);
                }
            };
            imgTask.setOnSucceeded(e -> {
                Image img = imgTask.getValue();
                if (img != null && !img.isError()) {
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(w);
                    iv.setFitHeight(h);
                    iv.setPreserveRatio(false);
                    // Rounded top corners clip
                    Rectangle clip = new Rectangle(w, h);
                    clip.setArcWidth(10);
                    clip.setArcHeight(10);
                    iv.setClip(clip);
                    Platform.runLater(() -> pane.getChildren().setAll(iv));
                }
            });
            daemon(imgTask);
        }
        return pane;
    }

    // ── Helper: re-enable Add buttons after a library remove ─────────────────

    private void refreshResultsAddButtons() {
        // Walk existing result cards and update their add buttons
        for (javafx.scene.Node node : resultsPane.getChildren()) {
            if (!(node instanceof VBox)) continue;
            VBox card = (VBox) node;
            // The VBox children are: coverPane, infoVBox
            if (card.getChildren().size() < 2) continue;
            VBox info = (VBox) card.getChildren().get(1);
            for (javafx.scene.Node child : info.getChildren()) {
                if (child instanceof Button) {
                    Button btn = (Button) child;
                    // Find rawgId stored as UserData on the card
                    if (card.getUserData() instanceof Integer) {
                        int rawgId = (int) card.getUserData();
                        boolean saved = gameService.isGameSaved(userId, rawgId);
                        btn.setText(saved ? "✓ Saved" : "+ Add");
                        btn.setDisable(saved);
                        btn.setStyle(saved ? savedBtnStyle() : addBtnStyle());
                    }
                }
            }
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void handleBackToProfile(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/tn/esprit/demo/view/profile.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("TeamHub – Profile");
        stage.setScene(new Scene(root, 1200, 750));
        stage.show();
    }

    // ── UI state helpers ──────────────────────────────────────────────────────

    private void setSearchState(boolean running) {
        searchBtn.setDisable(running);
        searchBtn.setText(running ? "Searching…" : "Search");
        searchSpinner.setVisible(running);
        searchSpinner.setManaged(running);
    }

    private void showSearchStatus(String msg, boolean hidden) {
        if (hidden || msg.isEmpty()) {
            searchStatusLabel.setVisible(false);
            searchStatusLabel.setManaged(false);
            return;
        }
        searchStatusLabel.setText(msg);
        // red for errors, amber for warnings
        String colour = msg.startsWith("No games") ? "#f59e0b" : "#f87171";
        searchStatusLabel.setStyle("-fx-text-fill: " + colour + "; -fx-font-size: 11px; -fx-padding: 4 0 0 0;");
        searchStatusLabel.setVisible(true);
        searchStatusLabel.setManaged(true);
    }

    private void hideSearchStatus() {
        searchStatusLabel.setVisible(false);
        searchStatusLabel.setManaged(false);
    }

    private void showFavLimit() {
        favLimitLabel.setVisible(true);
        favLimitLabel.setManaged(true);
    }

    private void hideFavLimit() {
        favLimitLabel.setVisible(false);
        favLimitLabel.setManaged(false);
    }

    // ── Styles ────────────────────────────────────────────────────────────────

    private static String addBtnStyle() {
        return "-fx-background-color: #00E5FF; -fx-text-fill: #050814;" +
               "-fx-font-size: 11px; -fx-font-weight: bold;" +
               "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 0;";
    }

    private static String savedBtnStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #22c55e;" +
               "-fx-font-size: 11px; -fx-border-color: #22c55e;" +
               "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 6 0;";
    }

    private static String favOnStyle() {
        return "-fx-background-color: rgba(245,158,11,0.15); -fx-text-fill: #f59e0b;" +
               "-fx-font-size: 11px; -fx-border-color: #f59e0b;" +
               "-fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 0;";
    }

    private static String favOffStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #5a6478;" +
               "-fx-font-size: 11px; -fx-border-color: #243044;" +
               "-fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 0;";
    }

    private static String removeBtnStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #f87171;" +
               "-fx-font-size: 11px; -fx-border-color: #f87171;" +
               "-fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 5 0;";
    }

    private static String metacriticColour(int score) {
        if (score >= 75) return "#22c55e";
        if (score >= 50) return "#f59e0b";
        return "#f87171";
    }

    // ── Thread helper ─────────────────────────────────────────────────────────

    private static void daemon(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
