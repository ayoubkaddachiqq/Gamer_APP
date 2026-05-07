package tn.esprit.demo.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.demo.service.AvatarFolderConfig;

import java.io.File;
import java.util.function.Consumer;

/**
 * Controller for the avatar picker popup.
 *
 * Sections:
 *  1. Built-in presets  (bundled jar resources)
 *  2. Admin folder      (loaded from AvatarFolderConfig at runtime, if set)
 *  3. Upload your own   (FileChooser)
 *
 * Path conventions returned to caller:
 *   "builtin:avatarN.png"   → bundled resource
 *   "file:/absolute/path"   → local file URI (admin folder or user upload)
 */
public class AvatarPickerController {

    private static final int BUILTIN_COUNT = 8;

    @FXML private FlowPane avatarGrid;
    @FXML private VBox adminSection;       // shown only when admin folder has images
    @FXML private FlowPane adminGrid;
    @FXML private Label uploadPathLabel;
    @FXML private StackPane uploadPreviewPane;
    @FXML private ImageView uploadPreviewImage;

    private String selectedPath = null;
    private StackPane selectedTile = null;
    private Consumer<String> onAvatarChosen;

    public void setOnAvatarChosen(Consumer<String> callback) {
        this.onAvatarChosen = callback;
    }

    @FXML
    public void initialize() {
        buildPresetGrid();
        buildAdminGrid();
    }

    // ── Built-in presets ─────────────────────────────────────────────────────

    private void buildPresetGrid() {
        for (int i = 1; i <= BUILTIN_COUNT; i++) {
            final String path = "builtin:avatar" + i + ".png";
            String res = "/tn/esprit/demo/view/avatars/avatar" + i + ".png";
            ImageView iv = makeImageView(72);
            try {
                iv.setImage(new Image(AvatarPickerController.class.getResourceAsStream(res)));
            } catch (Exception ignored) { }
            avatarGrid.getChildren().add(makeTile(iv, path));
        }
    }

    // ── Admin-configured folder ───────────────────────────────────────────────

    private void buildAdminGrid() {
        File[] files = AvatarFolderConfig.listAvatarFiles();
        if (files.length == 0) {
            adminSection.setVisible(false);
            adminSection.setManaged(false);
            return;
        }
        adminSection.setVisible(true);
        adminSection.setManaged(true);
        for (File f : files) {
            final String path = f.toURI().toString();
            ImageView iv = makeImageView(72);
            try { iv.setImage(new Image(path, 72, 72, true, true)); }
            catch (Exception ignored) { }
            adminGrid.getChildren().add(makeTile(iv, path));
        }
    }

    // ── Tile factory ─────────────────────────────────────────────────────────

    private StackPane makeTile(ImageView iv, String path) {
        StackPane tile = new StackPane(iv);
        tile.setPrefSize(80, 80);
        tile.setMaxSize(80, 80);
        tile.setStyle(normalStyle());
        tile.setOnMouseClicked(e -> selectTile(tile, path));
        tile.setOnMouseEntered(e -> { if (tile != selectedTile) tile.setStyle(hoverStyle()); });
        tile.setOnMouseExited(e -> { if (tile != selectedTile) tile.setStyle(normalStyle()); });
        return tile;
    }

    private static ImageView makeImageView(double size) {
        ImageView iv = new ImageView();
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(false);  // force square fill so any image fills the circle
        iv.setClip(new javafx.scene.shape.Circle(size / 2, size / 2, size / 2));
        return iv;
    }

    private void selectTile(StackPane tile, String path) {
        if (selectedTile != null) selectedTile.setStyle(normalStyle());
        selectedTile = tile;
        tile.setStyle(selectedStyle());
        selectedPath = path;
        uploadPreviewPane.setVisible(false);
        uploadPreviewPane.setManaged(false);
        uploadPathLabel.setText("No file selected");
    }

    // ── Upload ───────────────────────────────────────────────────────────────

    @FXML
    private void handleUpload() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Profile Picture");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        File file = fc.showOpenDialog(uploadPathLabel.getScene().getWindow());
        if (file == null) return;

        String uri = file.toURI().toString();
        selectedPath = uri;
        if (selectedTile != null) { selectedTile.setStyle(normalStyle()); selectedTile = null; }

        try {
            uploadPreviewImage.setPreserveRatio(false);
            uploadPreviewImage.setClip(new javafx.scene.shape.Circle(30, 30, 30));
            uploadPreviewImage.setImage(new Image(uri, true));
            uploadPreviewPane.setVisible(true);
            uploadPreviewPane.setManaged(true);
        } catch (Exception ignored) { }
        uploadPathLabel.setText(file.getName());
    }

    // ── Footer ───────────────────────────────────────────────────────────────

    @FXML
    private void handleApply() {
        if (selectedPath != null && onAvatarChosen != null) onAvatarChosen.accept(selectedPath);
        close();
    }

    @FXML
    private void handleCancel() { close(); }

    private void close() {
        ((Stage) avatarGrid.getScene().getWindow()).close();
    }

    // ── Styles ───────────────────────────────────────────────────────────────

    private static String normalStyle() {
        return "-fx-background-color:#0a1628;-fx-background-radius:40;" +
               "-fx-border-color:#1e2d45;-fx-border-radius:40;-fx-border-width:2;-fx-cursor:hand;";
    }
    private static String hoverStyle() {
        return "-fx-background-color:#0a2230;-fx-background-radius:40;" +
               "-fx-border-color:#00E5FF;-fx-border-radius:40;-fx-border-width:2;-fx-cursor:hand;";
    }
    private static String selectedStyle() {
        return "-fx-background-color:rgba(0,229,255,0.15);-fx-background-radius:40;" +
               "-fx-border-color:#00E5FF;-fx-border-radius:40;-fx-border-width:2.5;" +
               "-fx-effect:dropshadow(gaussian,#00E5FF88,12,0.4,0,0);-fx-cursor:hand;";
    }
}
