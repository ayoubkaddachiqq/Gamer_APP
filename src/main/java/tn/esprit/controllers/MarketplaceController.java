package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.model.Panier;
import tn.esprit.model.Produit;
import tn.esprit.model.ProduitPanier;
import tn.esprit.service.PanierService;
import tn.esprit.service.ProduitService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public class MarketplaceController {

    @FXML private GridPane productsGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private VBox panierItemsContainer;
    @FXML private ComboBox<Panier> panierComboBox;
    @FXML private TextField referenceField;
    @FXML private Label panierTotalLabel;
    @FXML private Button refreshButton;
    @FXML private Button createButton;
    @FXML private Button createPanierButton;
    @FXML private Button clearPanierButton;

    private final ProduitService produitService = new ProduitService();
    private final PanierService panierService = new PanierService();

    private final ObservableList<Produit> allProduits = FXCollections.observableArrayList();
    private final ObservableList<Produit> filteredProduits = FXCollections.observableArrayList();
    private final ObservableList<Panier> paniers = FXCollections.observableArrayList();
    private final ObservableList<ProduitPanier> lignesPanier = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupSortOptions();
        setupEventHandlers();
        loadData();
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
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
        categoryComboBox.setOnAction(e -> applyFilters());
        sortComboBox.setOnAction(e -> applyFilters());

        createButton.setOnAction(e -> showCreateProductDialog());
        createPanierButton.setOnAction(e -> createPanier());
        panierComboBox.setOnAction(e -> loadSelectedPanier());
        clearPanierButton.setOnAction(e -> clearSelectedPanier());
    }

    private void loadData() {
        try {
            allProduits.setAll(produitService.listProduits());

            List<String> categories = allProduits.stream()
                    .map(Produit::getCategorie)
                    .filter(cat -> cat != null && !cat.isEmpty())
                    .distinct()
                    .sorted()
                    .toList();
            ObservableList<String> categoryList = FXCollections.observableArrayList("All");
            categoryList.addAll(categories);
            categoryComboBox.setItems(categoryList);
            categoryComboBox.getSelectionModel().selectFirst();

            List<Panier> loadedPaniers = panierService.listPaniers();
            paniers.setAll(loadedPaniers);
            panierComboBox.setItems(paniers);
            if (!loadedPaniers.isEmpty()) {
                panierComboBox.getSelectionModel().selectFirst();
                loadSelectedPanier();
            }

            applyFilters();
        } catch (RuntimeException exception) {
            showError("Unable to load data", exception.getMessage());
        }
    }

    private void applyFilters() {
        String search = searchField.getText().toLowerCase(Locale.ROOT);
        String selectedCategory = categoryComboBox.getValue();

        List<Produit> filtered = allProduits.stream()
                .filter(p -> selectedCategory == null || selectedCategory.equals("All") ||
                        (p.getCategorie() != null && p.getCategorie().equalsIgnoreCase(selectedCategory)))
                .filter(p -> search.isEmpty() ||
                        containsIgnoreCase(p.getNom(), search) ||
                        containsIgnoreCase(p.getCategorie(), search) ||
                        containsIgnoreCase(p.getDescription(), search))
                .sorted(getComparator())
                .toList();

        filteredProduits.setAll(filtered);
        displayProductCards();
    }

    private java.util.Comparator<Produit> getComparator() {
        String sort = sortComboBox.getValue();
        if ("Name (Z-A)".equals(sort)) {
            return (a, b) -> b.getNom().compareTo(a.getNom());
        }
        if ("Price (Low-High)".equals(sort)) {
            return (a, b) -> a.getPrix().compareTo(b.getPrix());
        }
        if ("Price (High-Low)".equals(sort)) {
            return (a, b) -> b.getPrix().compareTo(a.getPrix());
        }
        if ("Stock (Low-High)".equals(sort)) {
            return java.util.Comparator.comparingInt(Produit::getStock);
        }
        if ("Stock (High-High)".equals(sort)) {
            return java.util.Comparator.comparingInt(Produit::getStock).reversed();
        }
        return (a, b) -> a.getNom().compareTo(b.getNom());
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    // ──────────────────────────────────────────────
    // Product card grid
    // ──────────────────────────────────────────────

    private void displayProductCards() {
        productsGrid.getChildren().clear();
        int row = 0;
        int col = 0;
        for (Produit produit : filteredProduits) {
            VBox card = createProductCard(produit);
            productsGrid.add(card, col, row);
            col++;
            if (col == 2) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createProductCard(Produit produit) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-border-color: #e0e0e0; -fx-border-width: 1; " +
                "-fx-background-color: #ffffff; -fx-background-radius: 12; " +
                "-fx-padding: 16; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, #00000015, 4, 0, 0, 2);"
        );
        card.setPrefHeight(220);

        Label imageLabel = new Label("\uD83D\uDCE6");
        imageLabel.setStyle("-fx-font-size: 48; -fx-alignment: center;");
        imageLabel.setMaxWidth(Double.MAX_VALUE);
        imageLabel.setAlignment(Pos.CENTER);

        Label nameLabel = new Label(produit.getNom());
        nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1a1a1a; -fx-wrap-text: true;");
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setMaxWidth(Double.MAX_VALUE);

        Label priceLabel = new Label("$" + (produit.getPrix() == null ? "0.00" : produit.getPrix().toPlainString()));
        priceLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2563eb; -fx-alignment: center;");

        String stockColor = produit.getStock() <= 5 ? "#dc2626" : "#4CAF50";
        Label stockLabel = new Label("Stock: " + produit.getStock());
        stockLabel.setStyle("-fx-font-size: 12; -fx-text-fill: " + stockColor + "; -fx-alignment: center;");

        card.setOnMouseClicked(e -> showProductDetailDialog(produit));
        card.getChildren().addAll(imageLabel, nameLabel, priceLabel, stockLabel);
        return card;
    }

    // ──────────────────────────────────────────────
    // Product detail dialog (click on card)
    // ──────────────────────────────────────────────

    private void showProductDetailDialog(Produit produit) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(produit.getNom());

        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #ffffff;");

        Label emojiLabel = new Label("\uD83D\uDCE6");
        emojiLabel.setStyle("-fx-font-size: 72; -fx-alignment: center;");
        emojiLabel.setMaxWidth(Double.MAX_VALUE);
        emojiLabel.setAlignment(Pos.CENTER);

        Label nameLabel = new Label(produit.getNom());
        nameLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #1a1a1a; -fx-alignment: center;");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setAlignment(Pos.CENTER);

        GridPane details = new GridPane();
        details.setHgap(15);
        details.setVgap(10);
        details.setPadding(new Insets(10, 0, 10, 0));

        int r = 0;
        addDetailRow(details, r++, "Description", produit.getDescription() != null ? produit.getDescription() : "-");
        addDetailRow(details, r++, "Price", "$" + (produit.getPrix() == null ? "0.00" : produit.getPrix().toPlainString()));
        addDetailRow(details, r++, "Stock", String.valueOf(produit.getStock()));
        addDetailRow(details, r++, "Category", produit.getCategorie() != null ? produit.getCategorie() : "-");
        addDetailRow(details, r++, "Status", produit.isActif() ? "Active" : "Inactive");

        Label qtyLabel = new Label("Quantity:");
        qtyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        Spinner<Integer> qtySpinner = new Spinner<>();
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Math.max(1, produit.getStock()), 1));
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(100);

        HBox qtyRow = new HBox(10, qtyLabel, qtySpinner);
        qtyRow.setAlignment(Pos.CENTER_LEFT);

        ComboBox<Panier> cartCombo = new ComboBox<>(paniers);
        cartCombo.setPromptText("Select a cart");
        cartCombo.setMaxWidth(Double.MAX_VALUE);
        cartCombo.setCellFactory(lv -> new ListCell<Panier>() {
            @Override
            protected void updateItem(Panier p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.getReference() + "  #" + p.getId());
            }
        });
        cartCombo.setButtonCell(new ListCell<Panier>() {
            @Override
            protected void updateItem(Panier p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.getReference() + "  #" + p.getId());
            }
        });
        if (panierComboBox.getValue() != null) {
            cartCombo.getSelectionModel().select(panierComboBox.getValue());
        }

        Button addButton = new Button("Add to Cart");
        addButton.setStyle("-fx-padding: 12 30; -fx-font-weight: bold; -fx-font-size: 14; -fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6;");
        addButton.setPrefWidth(Double.MAX_VALUE);
        addButton.setOnAction(e -> {
            Panier panier = cartCombo.getValue();
            if (panier == null) {
                showError("Error", "Please select a cart first.");
                return;
            }
            try {
                int qty = Integer.parseInt(qtySpinner.getEditor().getText().trim());
                panierService.addProduitToPanier(panier.getId(), produit.getId(), qty);
                loadSelectedPanier();
                dialog.close();
                showInfo("Success", "Product added to cart!");
            } catch (RuntimeException ex) {
                showError("Error", ex.getMessage());
            }
        });

        Button closeButton = new Button("Close");
        closeButton.setStyle("-fx-padding: 12 30; -fx-font-size: 14;");
        closeButton.setPrefWidth(Double.MAX_VALUE);
        closeButton.setOnAction(e -> dialog.close());

        root.getChildren().addAll(emojiLabel, nameLabel, details, qtyRow, cartCombo, addButton, closeButton);

        Scene scene = new Scene(root, 380, 520);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label + ":");
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #666;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 13; -fx-text-fill: #1a1a1a;");
        val.setWrapText(true);
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }

    // ──────────────────────────────────────────────
    // Create product dialog
    // ──────────────────────────────────────────────

    private void showCreateProductDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("New Product");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: #f9fafb;");

        TextField nomField = new TextField();
        TextArea descField = new TextArea();
        descField.setPrefRowCount(3);
        TextField prixField = new TextField();
        TextField stockField = new TextField();
        TextField catField = new TextField();
        CheckBox actifField = new CheckBox("Active");
        actifField.setSelected(true);

        int r = 0;
        form.add(new Label("Name:"), 0, r);
        form.add(nomField, 1, r++);
        form.add(new Label("Description:"), 0, r);
        form.add(descField, 1, r++);
        form.add(new Label("Price:"), 0, r);
        form.add(prixField, 1, r++);
        form.add(new Label("Stock:"), 0, r);
        form.add(stockField, 1, r++);
        form.add(new Label("Category:"), 0, r);
        form.add(catField, 1, r++);
        form.add(new Label(""), 0, r);
        form.add(actifField, 1, r++);

        Button saveButton = new Button("Save");
        saveButton.setStyle("-fx-padding: 10 24; -fx-font-weight: bold; -fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6;");
        Button cancelButton = new Button("Cancel");

        saveButton.setOnAction(e -> {
            try {
                if (nomField.getText().isBlank()) {
                    showError("Validation", "Product name is required");
                    return;
                }
                Produit produit = new Produit(
                        nomField.getText().trim(),
                        descField.getText().trim(),
                        new BigDecimal(prixField.getText().trim()),
                        Integer.parseInt(stockField.getText().trim()),
                        catField.getText().trim(),
                        actifField.isSelected()
                );
                produitService.addProduit(produit);
                loadData();
                dialog.close();
                showInfo("Success", "Product created successfully!");
            } catch (RuntimeException ex) {
                showError("Error", ex.getMessage());
            }
        });

        cancelButton.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(10, saveButton, cancelButton);
        form.add(new Label(""), 0, r);
        form.add(buttons, 1, r);

        Scene scene = new Scene(form, 380, 350);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ──────────────────────────────────────────────
    // Panier / Cart logic
    // ──────────────────────────────────────────────

    private void createPanier() {
        try {
            Panier panier = new Panier();
            panier.setReference(referenceField.getText().isBlank() ? null : referenceField.getText());
            Panier created = panierService.createPanier(panier);
            paniers.add(created);
            panierComboBox.getSelectionModel().select(created);
            referenceField.clear();
            showInfo("Success", "Cart created successfully!");
        } catch (RuntimeException exception) {
            showError("Error", exception.getMessage());
        }
    }

    private void loadSelectedPanier() {
        try {
            Panier panier = panierComboBox.getValue();
            if (panier != null) {
                Panier fullPanier = panierService.getPanierDetails(panier.getId());
                lignesPanier.setAll(fullPanier.getLignes());
                panierTotalLabel.setText(String.format("Total: $%.2f", fullPanier.getTotal()));
            } else {
                lignesPanier.clear();
                panierTotalLabel.setText("Total: $0.00");
            }
            displayPanierCards();
        } catch (RuntimeException exception) {
            showError("Error", exception.getMessage());
        }
    }

    private void displayPanierCards() {
        panierItemsContainer.getChildren().clear();
        if (lignesPanier.isEmpty()) {
            Label emptyLabel = new Label("Cart is empty");
            emptyLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #999; -fx-padding: 20;");
            panierItemsContainer.getChildren().add(emptyLabel);
            return;
        }
        for (ProduitPanier ligne : lignesPanier) {
            VBox card = createPanierItemCard(ligne);
            panierItemsContainer.getChildren().add(card);
        }
    }

    private VBox createPanierItemCard(ProduitPanier ligne) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-border-color: #e0e0e0; -fx-border-width: 1; " +
                "-fx-background-color: #ffffff; -fx-background-radius: 10; " +
                "-fx-padding: 14; " +
                "-fx-effect: dropshadow(gaussian, #00000015, 3, 0, 0, 1);"
        );

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label emoji = new Label("\uD83D\uDCE6");
        emoji.setStyle("-fx-font-size: 36;");

        VBox info = new VBox(4);
        String productName = ligne.getProduit() != null ? ligne.getProduit().getNom() : "Product #" + ligne.getProduitId();
        Label nameLabel = new Label(productName);
        nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");

        Label priceLabel = new Label("$" + ligne.getPrixUnitaire() + " x " + ligne.getQuantite()
                + " = $" + ligne.getSousTotal());
        priceLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");

        info.getChildren().addAll(nameLabel, priceLabel);
        header.getChildren().addAll(emoji, info);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Quantity spinner row
        Spinner<Integer> qtySpinner = new Spinner<>();
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, ligne.getQuantite()));
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(80);

        Button updateBtn = new Button("Update");
        updateBtn.setStyle("-fx-padding: 6 14; -fx-font-weight: bold; -fx-font-size: 11; -fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 4;");
        updateBtn.setOnAction(e -> {
            try {
                int newQty = Integer.parseInt(qtySpinner.getEditor().getText().trim());
                panierService.updateQuantite(ligne.getPanierId(), ligne.getProduitId(), newQty);
                loadSelectedPanier();
                showInfo("Success", "Quantity updated!");
            } catch (RuntimeException ex) {
                showError("Error", ex.getMessage());
            }
        });

        Button removeBtn = new Button("Remove");
        removeBtn.setStyle("-fx-padding: 6 14; -fx-font-size: 11; -fx-text-fill: white; -fx-background-color: #dc2626; -fx-background-radius: 4;");
        removeBtn.setOnAction(e -> {
            try {
                panierService.removeProduitFromPanier(ligne.getPanierId(), ligne.getProduitId());
                loadSelectedPanier();
                showInfo("Success", "Item removed from cart!");
            } catch (RuntimeException ex) {
                showError("Error", ex.getMessage());
            }
        });

        Button buyBtn = new Button("Buy");
        buyBtn.setStyle("-fx-padding: 6 20; -fx-font-weight: bold; -fx-font-size: 11; -fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 4;");
        buyBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Purchase");
            alert.setHeaderText("Buy " + productName);
            alert.setContentText("Quantity: " + ligne.getQuantite() + "\nTotal: $" + ligne.getSousTotal() + "\n\nConfirm purchase?");
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    panierService.removeProduitFromPanier(ligne.getPanierId(), ligne.getProduitId());
                    loadSelectedPanier();
                    showInfo("Purchase Successful", productName + " purchased!");
                } catch (RuntimeException ex) {
                    showError("Error", ex.getMessage());
                }
            }
        });

        HBox actions = new HBox(8, new Label("Qty:"), qtySpinner, updateBtn, removeBtn, buyBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(header, actions);
        return card;
    }

    private void clearSelectedPanier() {
        Panier panier = panierComboBox.getValue();
        if (panier == null) {
            showError("Validation", "Please select a cart first");
            return;
        }
        if (!confirm("Clear selected cart?")) return;
        try {
            panierService.clearPanier(panier.getId());
            loadSelectedPanier();
            showInfo("Success", "Cart cleared!");
        } catch (RuntimeException exception) {
            showError("Error", exception.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm");
        alert.setHeaderText(message);
        alert.getButtonTypes().setAll(ButtonType.CANCEL, new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE));
        return alert.showAndWait()
                .filter(buttonType -> buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                .isPresent();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
