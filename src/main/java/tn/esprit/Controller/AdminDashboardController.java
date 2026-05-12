package tn.esprit.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.AuditEvent;
import tn.esprit.entities.User;
import tn.esprit.services.AuditService;
import tn.esprit.utils.MyDB;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardController {

    private final AuditService auditService = new AuditService();

    @FXML private Label totalUsersLabel;
    @FXML private Label totalPostsLabel;
    @FXML private VBox usersContainer;
    @FXML private VBox auditContainer;

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || currentUser.getRole() != tn.esprit.entities.UserRole.ADMIN) {
            showError("Access denied. Admin only.");
            return;
        }
        loadStats();
        loadUsers();
        loadAuditLogs();
    }

    private void loadStats() {
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) totalUsersLabel.setText(String.valueOf(rs.getInt(1)));
        } catch (Exception e) {
            System.err.println("Error loading user count: " + e.getMessage());
        }

        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement("SELECT COUNT(*) FROM posts");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) totalPostsLabel.setText(String.valueOf(rs.getInt(1)));
        } catch (Exception e) {
            System.err.println("Error loading post count: " + e.getMessage());
        }
    }

    private void loadUsers() {
        usersContainer.getChildren().clear();
        String sql = "SELECT id, username, email, role, status, created_at FROM users ORDER BY created_at DESC LIMIT 50";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                HBox row = new HBox(15);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: #1e293b; -fx-padding: 10 15; -fx-background-radius: 8; -fx-border-color: rgba(255,255,255,0.05); -fx-border-radius: 8;");

                Label idLabel = new Label("#" + rs.getInt("id"));
                idLabel.setStyle("-fx-text-fill: #64748b; -fx-min-width: 40px;");

                Label nameLabel = new Label(rs.getString("username"));
                nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-min-width: 120px;");

                Label emailLabel = new Label(rs.getString("email"));
                emailLabel.setStyle("-fx-text-fill: #94a3b8; -fx-min-width: 180px;");

                Label roleLabel = new Label(rs.getString("role"));
                roleLabel.setStyle("-fx-text-fill: #4fc3f7; -fx-background-color: #1e3a5f; -fx-padding: 3 10; -fx-background-radius: 10;");

                Label statusLabel = new Label(rs.getString("status"));
                statusLabel.setStyle("-fx-text-fill: #22c55e;");

                row.getChildren().addAll(idLabel, nameLabel, emailLabel, roleLabel, statusLabel);
                usersContainer.getChildren().add(row);
            }
        } catch (Exception e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
    }

    private void loadAuditLogs() {
        auditContainer.getChildren().clear();
        List<AuditEvent> logs = auditService.getRecentLogs(50);
        for (AuditEvent event : logs) {
            HBox row = new HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #1e293b; -fx-padding: 8 12; -fx-background-radius: 6;");

            Label userLabel = new Label(event.getUsername());
            userLabel.setStyle("-fx-text-fill: #94a3b8; -fx-min-width: 100px;");

            Label actionLabel = new Label(event.getAction());
            actionLabel.setStyle("-fx-text-fill: #4fc3f7; -fx-min-width: 120px;");

            Label metaLabel = new Label(event.getMetadata());
            metaLabel.setStyle("-fx-text-fill: #64748b;");

            row.getChildren().addAll(userLabel, actionLabel, metaLabel);
            auditContainer.getChildren().add(row);
        }

        if (logs.isEmpty()) {
            Label empty = new Label("No audit logs available.");
            empty.setStyle("-fx-text-fill: #64748b;");
            auditContainer.getChildren().add(empty);
        }
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

    private void showError(String msg) {
        System.err.println(msg);
    }
}
