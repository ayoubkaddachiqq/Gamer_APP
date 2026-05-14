package tn.esprit.Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entities.Produit;
import tn.esprit.entities.User;
import tn.esprit.entities.UserRole;
import tn.esprit.services.PanierService;
import tn.esprit.services.ProduitService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class MarketplaceAdminController {

    @FXML private GridPane productsGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private Button refreshButton;
    @FXML private Button addProductButton;
    @FXML private Label totalProductsLabel;
    @FXML private Label totalCategoriesLabel;
    @FXML private Label totalCartsLabel;
    @FXML private Label lowStockLabel;
    @FXML private TableView<CategoryCount> categoryTable;
    @FXML private TableColumn<CategoryCount, String> catNameCol;
    @FXML private TableColumn<CategoryCount, Integer> catCountCol;
    @FXML private TableView<Produit> lowStockTable;
    @FXML private TableColumn<Produit, String> lowStockNameCol;
    @FXML private TableColumn<Produit, Integer> lowStockStockCol;
    @FXML private TableColumn<Produit, String> lowStockPriceCol;
    @FXML private Button adminButton;

    private final ProduitService produitService = new ProduitService();
    private final PanierService panierService = new PanierService();

    private final ObservableList<Produit> allProduits = FXCollections.observableArrayList();
    private final ObservableList<Produit> filteredProduits = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getCurrentUser();
        if (adminButton != null) {
            adminButton.setVisible(true);
            adminButton.setManaged(true);
        }
        setupSortOptions();
        setupEventHandlers();
        setupCategoryTable();
        setupLowStockTable();
        loadData();
    }

    private void setupCategoryTable() {
        catNameCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCountCol.setCellValueFactory(new PropertyValueFactory<>("count"));
        categoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupLowStockTable() {
        lowStockNameCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        lowStockStockCol.setCellValueFactory(new PropertyValueFactory<>("stock"));
        lowStockPriceCol.setCellValueFactory(data -> {
            BigDecimal p = data.getValue().getPrix();
            return new SimpleStringProperty("$" + (p == null ? "0.00" : p.toPlainString()));
        });
        lowStockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupSortOptions() {
        sortComboBox.setItems(FXCollections.observableArrayList(
                "Name (A-Z)", "Name (Z-A)", "Price (Low-High)", "Price (High-Low)",
                "Stock (Low-High)", "Stock (High-Low)"
        ));
        sortComboBox.getSelectionModel().selectFirst();
    }

    private void setupEventHandlers() {
        refreshButton.setOnAction(e -> loadData());
        searchField.textProperty().addListener((o, old, val) -> applyFilters());
        categoryComboBox.setOnAction(e -> applyFilters());
        sortComboBox.setOnAction(e -> applyFilters());
        addProductButton.setOnAction(e -> showProductForm(null));
    }

    private void loadData() {
        try {
            allProduits.setAll(produitService.getAll());
            updateCategoryFilter();
            applyFilters();
            loadStats();
        } catch (RuntimeException e) {
            showError("Error", e.getMessage());
        }
    }

    private void loadStats() {
        totalProductsLabel.setText(String.valueOf(allProduits.size()));
        long categoryCount = allProduits.stream()
                .map(Produit::getCategorie)
                .filter(c -> c != null && !c.isEmpty())
                .distinct().count();
        totalCategoriesLabel.setText(String.valueOf(categoryCount));
        int totalCarts;
        try {
            totalCarts = panierService.getAllPaniers().size();
        } catch (RuntimeException e) {
            totalCarts = 0;
        }
        totalCartsLabel.setText(String.valueOf(totalCarts));
        long lowStockCount = allProduits.stream()
                .filter(p -> p.getStock() <= 5)
                .count();
        lowStockLabel.setText(String.valueOf(lowStockCount));
        refreshCategoryTable();
        refreshLowStockTable();
    }

    private void refreshCategoryTable() {
        Map<String, Long> counts = allProduits.stream()
                .filter(p -> p.getCategorie() != null && !p.getCategorie().isEmpty())
                .collect(Collectors.groupingBy(Produit::getCategorie, Collectors.counting()));
        ObservableList<CategoryCount> rows = FXCollections.observableArrayList();
        counts.forEach((cat, cnt) -> rows.add(new CategoryCount(cat, cnt.intValue())));
        categoryTable.setItems(rows);
    }

    private void refreshLowStockTable() {
        List<Produit> lowStock = allProduits.stream()
                .filter(p -> p.getStock() <= 5)
                .toList();
        lowStockTable.setItems(FXCollections.observableArrayList(lowStock));
    }

    private void updateCategoryFilter() {
        String selected = categoryComboBox.getValue();
        List<String> cats = allProduits.stream()
                .map(Produit::getCategorie)
                .filter(c -> c != null && !c.isEmpty())
                .distinct().sorted().toList();
        categoryComboBox.getItems().setAll(new java.util.ArrayList<>());
        categoryComboBox.getItems().add("All");
        categoryComboBox.getItems().addAll(cats);
        if (selected != null && cats.contains(selected)) {
            categoryComboBox.setValue(selected);
        } else {
            categoryComboBox.getSelectionModel().selectFirst();
        }
    }

    private void applyFilters() {
        String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String cat = categoryComboBox.getValue() == null ? "All" : categoryComboBox.getValue();

        List<Produit> filtered = allProduits.stream()
                .filter(p -> cat.equals("All") || cat.equalsIgnoreCase(p.getCategorie()))
                .filter(p -> search.isEmpty()
                        || nul(p.getNom()).toLowerCase(Locale.ROOT).contains(search)
                        || nul(p.getCategorie()).toLowerCase(Locale.ROOT).contains(search)
                        || nul(p.getDescription()).toLowerCase(Locale.ROOT).contains(search))
                .sorted(getComparator())
                .toList();

        filteredProduits.setAll(filtered);
        displayCards();
    }

    private java.util.Comparator<Produit> getComparator() {
        String s = sortComboBox.getValue();
        if ("Name (Z-A)".equals(s)) return (a, b) -> b.getNom().compareTo(a.getNom());
        if ("Price (Low-High)".equals(s)) return (a, b) -> a.getPrix().compareTo(b.getPrix());
        if ("Price (High-Low)".equals(s)) return (a, b) -> b.getPrix().compareTo(a.getPrix());
        if ("Stock (Low-High)".equals(s)) return java.util.Comparator.comparingInt(Produit::getStock);
        if ("Stock (High-Low)".equals(s)) return java.util.Comparator.comparingInt(Produit::getStock).reversed();
        return (a, b) -> a.getNom().compareTo(b.getNom());
    }

    private String nul(String v) { return v == null ? "" : v; }

    private void displayCards() {
        productsGrid.getChildren().clear();
        int col = 0, row = 0;
        for (Produit p : filteredProduits) {
            productsGrid.add(createCard(p), col, row);
            if (++col == 3) { col = 0; row++; }
        }
    }

    private VBox createCard(Produit p) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: rgba(15, 23, 42, 0.9); -fx-border-color: rgba(216, 180, 254, 0.18); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 16; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 4);");
        card.setPrefHeight(200);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: rgba(25, 33, 55, 0.95); -fx-border-color: rgba(168, 85, 247, 0.4); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 16; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(168,85,247,0.2), 12, 0, 0, 6);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: rgba(15, 23, 42, 0.9); -fx-border-color: rgba(216, 180, 254, 0.18); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 16; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 4);"));

        Label emoji = new Label("\uD83D\uDCE6");
        emoji.setStyle("-fx-font-size: 38;");
        emoji.setMaxWidth(Double.MAX_VALUE);
        emoji.setAlignment(Pos.CENTER);

        Label name = new Label(p.getNom());
        name.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #f1f5f9; -fx-wrap-text: true;");
        name.setWrapText(true);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setAlignment(Pos.CENTER);

        Label price = new Label("$" + (p.getPrix() == null ? "0.00" : p.getPrix().toPlainString()));
        price.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #c084fc;");
        price.setMaxWidth(Double.MAX_VALUE);
        price.setAlignment(Pos.CENTER);

        String sc = p.getStock() <= 5 ? "#f87171" : "#4ade80";
        Label stock = new Label("Stock: " + p.getStock());
        stock.setStyle("-fx-font-size: 12; -fx-text-fill: " + sc + ";");
        stock.setMaxWidth(Double.MAX_VALUE);
        stock.setAlignment(Pos.CENTER);

        card.setOnMouseClicked(e -> showProductForm(p));
        card.getChildren().addAll(emoji, name, price, stock);
        return card;
    }

    private void showProductForm(Produit existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "Add Product" : "Edit Product");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0f172a; -fx-border-color: rgba(216, 180, 254, 0.25); -fx-border-radius: 10; -fx-background-radius: 10;");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);

        Label lblNom = new Label("Name:"); lblNom.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold;");
        TextField nomField = new TextField();
        nomField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f1f5f9; -fx-prompt-text-fill: #64748b;");

        Label lblDesc = new Label("Description:"); lblDesc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold;");
        TextArea descField = new TextArea();
        descField.setPrefRowCount(3);
        descField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f1f5f9; -fx-prompt-text-fill: #64748b;");

        Label lblPrix = new Label("Price:"); lblPrix.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold;");
        TextField prixField = new TextField();
        prixField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f1f5f9;");

        Label lblStock = new Label("Stock:"); lblStock.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold;");
        TextField stockField = new TextField();
        stockField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f1f5f9;");

        Label lblCat = new Label("Category:"); lblCat.setStyle("-fx-text-fill: #cbd5e1; -fx-font-weight: bold;");
        TextField catField = new TextField();
        catField.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f1f5f9;");

        CheckBox actifField = new CheckBox("Active");
        actifField.setStyle("-fx-text-fill: #cbd5e1;");
        actifField.setSelected(true);

        if (existing != null) {
            nomField.setText(nul(existing.getNom()));
            descField.setText(nul(existing.getDescription()));
            prixField.setText(existing.getPrix() == null ? "" : existing.getPrix().toPlainString());
            stockField.setText(String.valueOf(existing.getStock()));
            catField.setText(nul(existing.getCategorie()));
            actifField.setSelected(existing.isActif());
        }

        int r = 0;
        form.add(lblNom, 0, r); form.add(nomField, 1, r++);
        form.add(lblDesc, 0, r); form.add(descField, 1, r++);
        form.add(lblPrix, 0, r); form.add(prixField, 1, r++);
        form.add(lblStock, 0, r); form.add(stockField, 1, r++);
        form.add(lblCat, 0, r); form.add(catField, 1, r++);
        form.add(new Label(""), 0, r); form.add(actifField, 1, r++);

        Label title = new Label(existing == null ? "New Product" : "Edit Product");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");

        Button saveBtn = new Button("Save");
        saveBtn.setStyle("-fx-padding: 10 24; -fx-font-weight: bold; -fx-background-color: linear-gradient(to right, #7c3aed, #a855f7); -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-padding: 10 24; -fx-font-size: 13; -fx-background-color: rgba(148, 163, 184, 0.15); -fx-text-fill: #e2e8f0; -fx-background-radius: 6; -fx-cursor: hand;");
        HBox btns = new HBox(10, saveBtn, cancelBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);

        saveBtn.setOnAction(e -> {
            try {
                if (nomField.getText().isBlank()) {
                    showError("Validation", "Name is required");
                    return;
                }
                Produit produit = existing != null ? existing : new Produit();
                produit.setNom(nomField.getText().trim());
                produit.setDescription(descField.getText().trim());
                produit.setPrix(new BigDecimal(prixField.getText().trim()));
                produit.setStock(Integer.parseInt(stockField.getText().trim()));
                produit.setCategorie(catField.getText().trim());
                produit.setActif(actifField.isSelected());
                if (existing == null) {
                    User currentUser = SessionManager.getCurrentUser();
                    produit.setUserId(currentUser != null ? currentUser.getId() : 0);
                    produitService.ajouter(produit);
                } else {
                    produitService.modifier(produit);
                }
                loadData();
                dialog.close();
                showInfo("Success", "Product saved!");
            } catch (RuntimeException ex) {
                showError("Error", ex.getMessage());
            }
        });
        cancelBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(title, form, btns);
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        Scene dialogScene = new Scene(scroll, 420, 440);
        dialog.setScene(dialogScene);
        dialog.showAndWait();
    }

    // ─── NAVIGATION ───

    @FXML private void handleHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainInterface.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setMinWidth(1100); stage.setMinHeight(700);
            stage.setTitle("Team Hub - E-Sport Recruitment"); stage.setMaximized(true); stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleMyPosts(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MyPosts.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setMinWidth(1100); stage.setMinHeight(700);
            stage.setTitle("My Posts - Team Hub"); stage.setMaximized(true); stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleAnnonces(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Annonces.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Gestion des Annonces"); stage.setMaximized(true); stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleEvenements(ActionEvent event) {
        try {
            User currentUser = SessionManager.getCurrentUser();
            boolean isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;
            String fxml = isAdmin ? "/views/EvenementsAdmin.fxml" : "/views/Evenements.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Gestion des Evenements"); stage.setMaximized(true); stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Profile.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Team Hub - Profile"); stage.setMaximized(true); stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleMarketplace(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MarketplaceAdmin.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Marketplace Admin"); stage.setMaximized(true); stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleAdmin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AdminDashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setMinWidth(1100); stage.setMinHeight(700);
            stage.setTitle("Admin Dashboard - Team Hub"); stage.setMaximized(true); stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleLogout(ActionEvent event) throws IOException {
        SessionManager.logout();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 700));
        stage.setTitle("Team Hub - Login"); stage.setMaximized(true); stage.show();
    }

    private void showError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t); a.setContentText(m); a.showAndWait();
    }

    private void showInfo(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setContentText(m); a.showAndWait();
    }

    public static class CategoryCount {
        private final String category;
        private final int count;
        public CategoryCount(String category, int count) { this.category = category; this.count = count; }
        public String getCategory() { return category; }
        public int getCount() { return count; }
    }
}
