package tn.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.model.Produit;
import tn.esprit.service.PanierService;
import tn.esprit.service.ProduitService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminController {

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
    @FXML private TableView<AdminController.CategoryCount> categoryTable;
    @FXML private TableColumn<AdminController.CategoryCount, String> catNameCol;
    @FXML private TableColumn<AdminController.CategoryCount, Integer> catCountCol;
    @FXML private TableView<Produit> lowStockTable;
    @FXML private TableColumn<Produit, String> lowStockNameCol;
    @FXML private TableColumn<Produit, Integer> lowStockStockCol;
    @FXML private TableColumn<Produit, String> lowStockPriceCol;

    private final ProduitService produitService = new ProduitService();
    private final PanierService panierService = new PanierService();

    private final ObservableList<Produit> allProduits = FXCollections.observableArrayList();
    private final ObservableList<Produit> filteredProduits = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
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
            java.math.BigDecimal p = data.getValue().getPrix();
            return new SimpleStringProperty("$" + (p == null ? "0.00" : p.toPlainString()));
        });
        lowStockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupSortOptions() {
        sortComboBox.setItems(FXCollections.observableArrayList(
                "Name (A-Z)", "Name (Z-A)", "Price (Low-High)", "Price (High-Low)",
                "Stock (Low-High)", "Stock (High-High)"
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
            allProduits.setAll(produitService.listProduits());
            updateCategoryFilter();
            applyFilters();
            loadStats();
        } catch (RuntimeException e) {
            showError("Error", e.getMessage());
        }
    }

    private void loadStats() {
        int totalProducts = allProduits.size();
        totalProductsLabel.setText(String.valueOf(totalProducts));

        long categoryCount = allProduits.stream()
                .map(Produit::getCategorie)
                .filter(c -> c != null && !c.isEmpty())
                .distinct().count();
        totalCategoriesLabel.setText(String.valueOf(categoryCount));

        int totalCarts;
        try {
            totalCarts = panierService.listPaniers().size();
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
        counts.forEach((cat, cnt) ->
                rows.add(new CategoryCount(cat, cnt.intValue())));
        categoryTable.setItems(rows);
    }

    private void refreshLowStockTable() {
        List<Produit> lowStock = allProduits.stream()
                .filter(p -> p.getStock() <= 5)
                .collect(Collectors.toList());
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
        if ("Stock (High-High)".equals(s)) return java.util.Comparator.comparingInt(Produit::getStock).reversed();
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
        card.setStyle(
                "-fx-background-color: #ffffff; -fx-border-color: #dce3ea; -fx-border-radius: 8; " +
                "-fx-background-radius: 8; -fx-padding: 16; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 4, 0, 0, 2);");
        card.setPrefHeight(200);

        Label emoji = new Label("\uD83D\uDCE6");
        emoji.setStyle("-fx-font-size: 42;");
        emoji.setMaxWidth(Double.MAX_VALUE);
        emoji.setAlignment(Pos.CENTER);

        Label name = new Label(p.getNom());
        name.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1f2933; -fx-wrap-text: true;");
        name.setWrapText(true);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setAlignment(Pos.CENTER);

        Label price = new Label("$" + (p.getPrix() == null ? "0.00" : p.getPrix().toPlainString()));
        price.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2563eb;");
        price.setMaxWidth(Double.MAX_VALUE);
        price.setAlignment(Pos.CENTER);

        String sc = p.getStock() <= 5 ? "#dc2626" : "#059669";
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

        VBox root = new VBox(12);
        root.setStyle("-fx-background-color: #ffffff;");
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #ffffff;");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);

        TextField nomField = new TextField();
        TextArea descField = new TextArea();
        descField.setPrefRowCount(3);
        TextField prixField = new TextField();
        TextField stockField = new TextField();
        TextField catField = new TextField();
        CheckBox actifField = new CheckBox("Active");
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
        form.add(new Label("Name:"), 0, r); form.add(nomField, 1, r++);
        form.add(new Label("Description:"), 0, r); form.add(descField, 1, r++);
        form.add(new Label("Price:"), 0, r); form.add(prixField, 1, r++);
        form.add(new Label("Stock:"), 0, r); form.add(stockField, 1, r++);
        form.add(new Label("Category:"), 0, r); form.add(catField, 1, r++);
        form.add(new Label(""), 0, r); form.add(actifField, 1, r++);

        Button saveBtn = new Button("Save");
        Button cancelBtn = new Button("Cancel");
        HBox btns = new HBox(10, saveBtn, cancelBtn);
        form.add(new Label(""), 0, r); form.add(btns, 1, r);

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

                if (existing != null) {
                    produitService.updateProduit(produit);
                } else {
                    produitService.addProduit(produit);
                }
                loadData();
                dialog.close();
                showInfo("Success", "Product saved!");
            } catch (RuntimeException ex) {
                showError("Error", ex.getMessage());
            }
        });
        cancelBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(
                new Label(existing == null ? "New Product" : "Edit Product") {{
                    setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1f2933;");
                }},
                form
        );

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        Scene dialogScene = new Scene(scroll, 420, 460);
        String css = getClass().getResource("/marketplace.css").toExternalForm();
        if (css != null) dialogScene.getStylesheets().add(css);
        dialog.setScene(dialogScene);
        dialog.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setContentText(msg); a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setContentText(msg); a.showAndWait();
    }

    public static class CategoryCount {
        private final String category;
        private final int count;

        public CategoryCount(String category, int count) {
            this.category = category;
            this.count = count;
        }

        public String getCategory() { return category; }
        public int getCount() { return count; }
    }
}
