package tn.esprit.demo.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import tn.esprit.demo.dao.AuditDao;
import tn.esprit.demo.dao.UserDao;
import tn.esprit.demo.db.DataSourceProvider;
import tn.esprit.demo.model.AuditEvent;
import tn.esprit.demo.model.User;
import tn.esprit.demo.model.UserRole;
import tn.esprit.demo.model.UserStatus;
import tn.esprit.demo.service.AvatarFolderConfig;
import tn.esprit.demo.service.SessionManager;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class AdminDashboardController {

    // ── Nav / header ──────────────────────────────────────────────────────────
    @FXML private Label adminNameLabel;
    @FXML private Label adminInitialsLabel;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;

    // ── Panels ────────────────────────────────────────────────────────────────
    @FXML private VBox overviewPanel;
    @FXML private VBox usersPanel;
    @FXML private VBox avatarPanel;
    @FXML private VBox activityPanel;

    // ── Overview stat labels ──────────────────────────────────────────────────
    @FXML private Label statTotalUsers;
    @FXML private Label statNewToday;
    @FXML private Label statActive;
    @FXML private Label statPending;
    @FXML private Label statLocked;
    @FXML private Label statLoginsToday;
    @FXML private Label statPlayers;
    @FXML private Label statTeams;
    @FXML private Label statAdmins;
    @FXML private VBox  activityPreviewBox;

    // ── Users panel ───────────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Label tableStatusLabel;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, String> colJoined;
    @FXML private TableColumn<User, String> colActions;

    // ── Avatar config panel ───────────────────────────────────────────────────
    @FXML private TextField folderPathField;
    @FXML private Label folderStatusLabel;
    @FXML private Label folderCountLabel;
    @FXML private FlowPane folderPreviewPane;
    @FXML private Label folderEmptyLabel;

    // ── Activity log panel ────────────────────────────────────────────────────
    @FXML private VBox activityLogBox;

    // ── DAOs ──────────────────────────────────────────────────────────────────
    private final UserDao userDao   = new UserDao(DataSourceProvider.getDataSource());
    private final AuditDao auditDao = new AuditDao(DataSourceProvider.getDataSource());

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd MMM HH:mm").withZone(ZoneId.systemDefault());

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        User admin = SessionManager.getCurrentUser();
        if (admin != null) {
            adminNameLabel.setText(admin.getUsername());
            String init = admin.getUsername().length() >= 2
                    ? admin.getUsername().substring(0, 2).toUpperCase() : "AD";
            adminInitialsLabel.setText(init);
        }
        setupUserTable();
        showOverview(null);
    }

    // ── Panel switching ───────────────────────────────────────────────────────

    @FXML public void showOverview(ActionEvent e)  { switchPanel("Overview",     "Platform health at a glance", overviewPanel); refreshOverview(); }
    @FXML public void showUsers(ActionEvent e)     { switchPanel("Users",        "Manage all registered accounts", usersPanel); loadAllUsers(); }
    @FXML public void showAvatarConfig(ActionEvent e) { switchPanel("Avatar Config", "Set the shared avatar folder for players", avatarPanel); loadAvatarConfig(); }
    @FXML public void showActivity(ActionEvent e)  { switchPanel("Activity Log", "Last 50 platform events", activityPanel); loadActivity(); }

    private void switchPanel(String title, String subtitle, VBox show) {
        pageTitleLabel.setText(title);
        pageSubtitleLabel.setText(subtitle);
        for (VBox p : new VBox[]{overviewPanel, usersPanel, avatarPanel, activityPanel}) {
            p.setVisible(p == show);
            p.setManaged(p == show);
        }
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @FXML
    private void handleRefresh() {
        if (overviewPanel.isVisible())   refreshOverview();
        else if (usersPanel.isVisible()) loadAllUsers();
        else if (avatarPanel.isVisible()) loadAvatarConfig();
        else if (activityPanel.isVisible()) loadActivity();
    }

    // ── Overview ─────────────────────────────────────────────────────────────

    private void refreshOverview() {
        statTotalUsers.setText(String.valueOf(userDao.countTotal()));
        statNewToday.setText("+" + userDao.countNewToday() + " new today");
        statActive.setText(String.valueOf(userDao.countByStatus(UserStatus.ACTIVE)));
        statPending.setText(String.valueOf(userDao.countByStatus(UserStatus.PENDING)));
        statLocked.setText(String.valueOf(userDao.countByStatus(UserStatus.LOCKED)));
        statLoginsToday.setText(String.valueOf(auditDao.countTodayLogins()));
        statPlayers.setText(String.valueOf(userDao.countByRole(UserRole.PLAYER)));
        statTeams.setText(String.valueOf(userDao.countByRole(UserRole.TEAM)));
        statAdmins.setText(String.valueOf(userDao.countByRole(UserRole.ADMIN)));

        // activity preview — last 5 events
        activityPreviewBox.getChildren().clear();
        List<AuditEvent> events = auditDao.findRecent(5);
        for (AuditEvent ev : events) {
            activityPreviewBox.getChildren().add(buildActivityRow(ev, true));
        }
        if (events.isEmpty()) {
            Label empty = new Label("No activity recorded yet.");
            empty.setStyle("-fx-text-fill: #3d4a5c; -fx-font-size: 11px;");
            activityPreviewBox.getChildren().add(empty);
        }
    }

    // ── Users panel ───────────────────────────────────────────────────────────

    private void setupUserTable() {
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole().name()));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        colJoined.setCellValueFactory(d -> {
            if (d.getValue().getCreatedAt() == null) return new SimpleStringProperty("—");
            return new SimpleStringProperty(DT_FMT.format(d.getValue().getCreatedAt()));
        });

        // Colour status column
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String colour = switch (item) {
                    case "ACTIVE"  -> "#22c55e";
                    case "PENDING" -> "#f59e0b";
                    case "LOCKED"  -> "#f87171";
                    default        -> "#7a8899";
                };
                setStyle("-fx-text-fill: " + colour + "; -fx-font-weight: bold;");
            }
        });

        // Actions column: Lock / Unlock / Delete buttons
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button lockBtn   = makeActionBtn("Lock",   "#f59e0b");
            private final Button unlockBtn = makeActionBtn("Unlock", "#22c55e");
            private final Button delBtn    = makeActionBtn("Delete", "#f87171");
            {
                lockBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    userDao.updateStatus(u.getId(), UserStatus.LOCKED, u.isEmailVerified());
                    loadAllUsers();
                });
                unlockBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    userDao.updateStatus(u.getId(), UserStatus.ACTIVE, true);
                    loadAllUsers();
                });
                delBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Delete user \"" + u.getUsername() + "\"? This cannot be undone.",
                            ButtonType.OK, ButtonType.CANCEL);
                    confirm.setTitle("Confirm Delete");
                    confirm.setHeaderText(null);
                    confirm.showAndWait().ifPresent(btn -> {
                        if (btn == ButtonType.OK) {
                            userDao.deleteById(u.getId());
                            loadAllUsers();
                        }
                    });
                });
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                User u = getTableView().getItems().get(getIndex());
                // hide Lock if already locked, hide Unlock if already active/pending
                lockBtn.setVisible(u.getStatus() != UserStatus.LOCKED);
                lockBtn.setManaged(u.getStatus() != UserStatus.LOCKED);
                unlockBtn.setVisible(u.getStatus() == UserStatus.LOCKED);
                unlockBtn.setManaged(u.getStatus() == UserStatus.LOCKED);
                HBox box = new HBox(4, lockBtn, unlockBtn, delBtn);
                setGraphic(box);
            }
        });

        userTable.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-table-cell-border-color: #111c2e;" +
            "-fx-selection-bar: rgba(0,229,255,0.12);" +
            "-fx-selection-bar-non-focused: rgba(0,229,255,0.06);"
        );
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private Button makeActionBtn(String text, String colour) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: transparent; -fx-text-fill: " + colour + ";" +
                   "-fx-font-size: 11px; -fx-cursor: hand; -fx-border-color: " + colour + ";" +
                   "-fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 3 8;");
        return b;
    }

    private void loadAllUsers() {
        List<User> users = userDao.findAll();
        userTable.setItems(FXCollections.observableArrayList(users));
        tableStatusLabel.setText(users.size() + " user" + (users.size() == 1 ? "" : "s"));
    }

    @FXML
    private void handleSearch() {
        String q = searchField.getText().trim().toLowerCase();
        if (q.isEmpty()) { loadAllUsers(); return; }
        List<User> filtered = userDao.findAll().stream()
                .filter(u -> u.getUsername().toLowerCase().contains(q)
                          || u.getEmail().toLowerCase().contains(q))
                .collect(Collectors.toList());
        userTable.setItems(FXCollections.observableArrayList(filtered));
        tableStatusLabel.setText(filtered.size() + " result" + (filtered.size() == 1 ? "" : "s"));
    }

    @FXML
    private void handleShowAll() {
        searchField.clear();
        loadAllUsers();
    }

    // ── Avatar config ─────────────────────────────────────────────────────────

    private void loadAvatarConfig() {
        String current = AvatarFolderConfig.getFolder();
        folderPathField.setText(current != null ? current : "");
        refreshFolderPreview();
    }

    @FXML
    private void handleBrowseFolder() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Avatar Folder");
        String current = AvatarFolderConfig.getFolder();
        if (current != null) {
            File f = new File(current);
            if (f.isDirectory()) dc.setInitialDirectory(f);
        }
        File chosen = dc.showDialog(folderPathField.getScene().getWindow());
        if (chosen != null) folderPathField.setText(chosen.getAbsolutePath());
    }

    @FXML
    private void handleSaveFolder() {
        String path = folderPathField.getText().trim();
        if (!path.isEmpty()) {
            File dir = new File(path);
            if (!dir.isDirectory()) {
                setFolderStatus("⚠ Path does not exist or is not a folder.", "#f59e0b");
                return;
            }
        }
        AvatarFolderConfig.setFolder(path.isEmpty() ? null : path);
        setFolderStatus(path.isEmpty() ? "Folder cleared." : "✓ Saved. Players will see images from this folder.", "#22c55e");
        refreshFolderPreview();
    }

    @FXML
    private void handleClearFolder() {
        folderPathField.clear();
        AvatarFolderConfig.setFolder(null);
        setFolderStatus("Folder cleared.", "#5a6478");
        refreshFolderPreview();
    }

    private void setFolderStatus(String msg, String colour) {
        folderStatusLabel.setText(msg);
        folderStatusLabel.setStyle("-fx-text-fill: " + colour + "; -fx-font-size: 11px;");
    }

    private void refreshFolderPreview() {
        folderPreviewPane.getChildren().clear();
        File[] files = AvatarFolderConfig.listAvatarFiles();
        if (files.length == 0) {
            folderEmptyLabel.setVisible(true);
            folderEmptyLabel.setManaged(true);
            folderCountLabel.setText("");
            return;
        }
        folderEmptyLabel.setVisible(false);
        folderEmptyLabel.setManaged(false);
        folderCountLabel.setText(files.length + " image" + (files.length == 1 ? "" : "s") + " found");

        for (File f : files) {
            try {
                ImageView iv = new ImageView(new Image(f.toURI().toString(), 64, 64, false, true));
                iv.setFitWidth(64);
                iv.setFitHeight(64);
                iv.setPreserveRatio(false);   // force square fill regardless of aspect ratio
                iv.setClip(new Circle(32, 32, 32));

                StackPane tile = new StackPane(iv);
                tile.setPrefSize(70, 70);
                tile.setMaxSize(70, 70);
                tile.setStyle("-fx-background-color: #0a1628; -fx-background-radius: 35;" +
                              "-fx-border-color: #1e2d45; -fx-border-radius: 35; -fx-border-width: 2;");

                Tooltip tip = new Tooltip(f.getName());
                Tooltip.install(tile, tip);

                folderPreviewPane.getChildren().add(tile);
            } catch (Exception ignored) { }
        }
    }

    // ── Activity log ──────────────────────────────────────────────────────────

    private void loadActivity() {
        activityLogBox.getChildren().clear();
        List<AuditEvent> events = auditDao.findRecent(50);
        if (events.isEmpty()) {
            Label empty = new Label("No activity recorded yet.");
            empty.setStyle("-fx-text-fill: #3d4a5c; -fx-font-size: 12px; -fx-padding: 16;");
            activityLogBox.getChildren().add(empty);
            return;
        }
        for (AuditEvent ev : events) {
            activityLogBox.getChildren().add(buildActivityRow(ev, false));
        }
    }

    private Node buildActivityRow(AuditEvent ev, boolean compact) {
        // Action badge colour
        String badgeColour = switch (ev.getAction()) {
            case "LOGIN_SUCCESS" -> "#22c55e";
            case "LOGIN_FAILED"  -> "#f87171";
            case "REGISTER"      -> "#00E5FF";
            case "EMAIL_VERIFIED"-> "#a78bfa";
            default              -> "#5a6478";
        };

        Label badge = new Label(ev.getAction().replace("_", " "));
        badge.setStyle("-fx-background-color: transparent;" +
                       "-fx-text-fill: " + badgeColour + ";" +
                       "-fx-font-size: " + (compact ? "10" : "11") + "px;" +
                       "-fx-font-weight: bold;" +
                       "-fx-border-color: " + badgeColour + ";" +
                       "-fx-border-radius: 4; -fx-background-radius: 4;" +
                       "-fx-padding: 2 6;");

        Label userLbl = new Label(ev.getUsername() != null ? ev.getUsername() : "system");
        userLbl.setStyle("-fx-text-fill: #dde4f0; -fx-font-size: " + (compact ? "11" : "12") + "px;");
        userLbl.setPrefWidth(compact ? 100 : 130);

        Label timeLbl = new Label(ev.getCreatedAt() != null ? DT_FMT.format(ev.getCreatedAt()) : "");
        timeLbl.setStyle("-fx-text-fill: #3d4a5c; -fx-font-size: 10px;");

        Label metaLbl = new Label(ev.getMetadata() != null ? ev.getMetadata() : "");
        metaLbl.setStyle("-fx-text-fill: #5a6478; -fx-font-size: 10px;");
        javafx.scene.layout.HBox.setHgrow(metaLbl, javafx.scene.layout.Priority.ALWAYS);

        HBox row = new HBox(10, badge, userLbl, metaLbl, timeLbl);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: " + (compact ? "4 0" : "9 20") + ";" +
                     (compact ? "" : "-fx-border-color: #111c2e; -fx-border-width: 0 0 1 0;"));
        return row;
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @FXML
    private void handleLogout(ActionEvent event) throws IOException {
        SessionManager.clear();
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/tn/esprit/demo/view/login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("TeamHub – Login");
        stage.setScene(new Scene(root, 1000, 700));
        stage.show();
    }
}
