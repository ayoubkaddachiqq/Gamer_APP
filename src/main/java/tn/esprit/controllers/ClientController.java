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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.model.Panier;
import tn.esprit.model.Produit;
import tn.esprit.model.ProduitPanier;
import tn.esprit.service.PanierService;
import tn.esprit.service.ProduitService;
import tn.esprit.service.StripeService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

public class ClientController {

    @FXML private GridPane productsGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private VBox panierItemsContainer;
    @FXML private ComboBox<Panier> panierComboBox;
    @FXML private TextField referenceField;
    @FXML private Label panierTotalLabel;
    @FXML private Button refreshButton;
    @FXML private Button createPanierButton;
    @FXML private Button clearPanierButton;

    private final ProduitService produitService = new ProduitService();
    private final PanierService panierService = new PanierService();
    private final StripeService stripeService = new StripeService();

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
        searchField.textProperty().addListener((o, old, v) -> applyFilters());
        categoryComboBox.setOnAction(e -> applyFilters());
        sortComboBox.setOnAction(e -> applyFilters());
        createPanierButton.setOnAction(e -> createPanier());
        panierComboBox.setOnAction(e -> loadSelectedPanier());
        clearPanierButton.setOnAction(e -> clearSelectedPanier());
    }

    private void loadData() {
        try {
            allProduits.setAll(produitService.listProduits());
            updateCategoryFilter();
            List<Panier> loaded = panierService.listPaniers();
            paniers.setAll(loaded);
            panierComboBox.setItems(paniers);
            if (!loaded.isEmpty()) {
                panierComboBox.getSelectionModel().selectFirst();
                loadSelectedPanier();
            }
            applyFilters();
        } catch (RuntimeException e) {
            showError("Error", e.getMessage());
        }
    }

    private void updateCategoryFilter() {
        String sel = categoryComboBox.getValue();
        List<String> cats = allProduits.stream()
                .map(Produit::getCategorie)
                .filter(c -> c != null && !c.isEmpty())
                .distinct().sorted().toList();
        categoryComboBox.getItems().setAll(new java.util.ArrayList<>());
        categoryComboBox.getItems().add("All");
        categoryComboBox.getItems().addAll(cats);
        categoryComboBox.getSelectionModel().select(sel != null && cats.contains(sel) ? sel : "All");
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

    // ──────────── PRODUCT CARDS ────────────

    private void displayCards() {
        productsGrid.getChildren().clear();
        int col = 0, row = 0;
        for (Produit p : filteredProduits) {
            productsGrid.add(createCard(p), col, row);
            if (++col == 2) { col = 0; row++; }
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

        card.setOnMouseClicked(e -> showDetailDialog(p));
        card.getChildren().addAll(emoji, name, price, stock);
        return card;
    }

    // ──────────── PRODUCT DETAIL + ADD TO CART ────────────

    private void showDetailDialog(Produit produit) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(produit.getNom());

        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #ffffff;");

        Label emoji = new Label("\uD83D\uDCE6");
        emoji.setStyle("-fx-font-size: 64;");
        emoji.setMaxWidth(Double.MAX_VALUE);
        emoji.setAlignment(Pos.CENTER);

        Label name = new Label(produit.getNom());
        name.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #1f2933;");
        name.setMaxWidth(Double.MAX_VALUE);
        name.setAlignment(Pos.CENTER);

        GridPane details = new GridPane();
        details.setHgap(15);
        details.setVgap(10);
        details.setPadding(new Insets(10, 0, 10, 0));

        int r = 0;
        addRow(details, r++, "Description", nul(produit.getDescription()));
        addRow(details, r++, "Price", "$" + (produit.getPrix() == null ? "0.00" : produit.getPrix().toPlainString()));
        addRow(details, r++, "Stock", String.valueOf(produit.getStock()));
        addRow(details, r++, "Category", nul(produit.getCategorie()));
        addRow(details, r++, "Status", produit.isActif() ? "Active" : "Inactive");

        Label qtyLbl = new Label("Quantity:");
        qtyLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #1f2933;");
        Spinner<Integer> qtySpinner = new Spinner<>();
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Math.max(1, produit.getStock()), 1));
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(100);

        HBox qtyRow = new HBox(10, qtyLbl, qtySpinner);
        qtyRow.setAlignment(Pos.CENTER_LEFT);

        ComboBox<Panier> cartCombo = new ComboBox<>(paniers);
        cartCombo.setPromptText("Select a cart");
        cartCombo.setMaxWidth(Double.MAX_VALUE);
        cartCombo.setCellFactory(lv -> new ListCell<Panier>() {
            protected void updateItem(Panier p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.getReference() + "  #" + p.getId());
            }
        });
        cartCombo.setButtonCell(new ListCell<Panier>() {
            protected void updateItem(Panier p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.getReference() + "  #" + p.getId());
            }
        });
        if (panierComboBox.getValue() != null) {
            cartCombo.getSelectionModel().select(panierComboBox.getValue());
        }

        Button addBtn = new Button("Add to Cart");
        addBtn.setStyle("-fx-padding: 12 30; -fx-font-weight: bold; -fx-font-size: 14; -fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6;");
        addBtn.setPrefWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> {
            Panier panier = cartCombo.getValue();
            if (panier == null) { showError("Error", "Select a cart first."); return; }
            try {
                int qty = Integer.parseInt(qtySpinner.getEditor().getText().trim());
                panierService.addProduitToPanier(panier.getId(), produit.getId(), qty);
                loadSelectedPanier();
                dialog.close();
                showInfo("Success", "Product added to cart!");
            } catch (RuntimeException ex) { showError("Error", ex.getMessage()); }
        });

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-padding: 12 30; -fx-font-size: 14;");
        closeBtn.setPrefWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(emoji, name, details, qtyRow, cartCombo, addBtn, closeBtn);
        Scene dialogScene = new Scene(root, 380, 500);
        String css = getClass().getResource("/marketplace.css").toExternalForm();
        if (css != null) dialogScene.getStylesheets().add(css);
        dialog.setScene(dialogScene);
        dialog.showAndWait();
    }

    private void addRow(GridPane g, int row, String label, String value) {
        Label l = new Label(label + ":");
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #6b7280;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 13; -fx-text-fill: #1f2933;");
        v.setWrapText(true);
        g.add(l, 0, row);
        g.add(v, 1, row);
    }

    // ──────────── PANIER / CART ────────────

    private void createPanier() {
        try {
            Panier panier = new Panier();
            panier.setReference(referenceField.getText().isBlank() ? null : referenceField.getText());
            Panier created = panierService.createPanier(panier);
            paniers.add(created);
            panierComboBox.getSelectionModel().select(created);
            referenceField.clear();
            showInfo("Success", "Cart created!");
        } catch (RuntimeException e) { showError("Error", e.getMessage()); }
    }

    private void loadSelectedPanier() {
        try {
            Panier panier = panierComboBox.getValue();
            if (panier != null) {
                Panier full = panierService.getPanierDetails(panier.getId());
                lignesPanier.setAll(full.getLignes());
                panierTotalLabel.setText(String.format("Total: $%.2f", full.getTotal()));
            } else {
                lignesPanier.clear();
                panierTotalLabel.setText("Total: $0.00");
            }
            displayPanierCards();
        } catch (RuntimeException e) { showError("Error", e.getMessage()); }
    }

    private void displayPanierCards() {
        panierItemsContainer.getChildren().clear();
        if (lignesPanier.isEmpty()) {
            Label empty = new Label("Cart is empty");
            empty.setStyle("-fx-font-size: 14; -fx-text-fill: #6b7280; -fx-padding: 20;");
            panierItemsContainer.getChildren().add(empty);
            return;
        }
        for (ProduitPanier ligne : lignesPanier) {
            panierItemsContainer.getChildren().add(createPanierCard(ligne));
        }
    }

    private VBox createPanierCard(ProduitPanier ligne) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color: #ffffff; -fx-border-color: #dce3ea; -fx-border-radius: 6; " +
                "-fx-background-radius: 6; -fx-padding: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 3, 0, 0, 1);");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label emoji = new Label("\uD83D\uDCE6");
        emoji.setStyle("-fx-font-size: 32;");

        String productName = ligne.getProduit() != null ? ligne.getProduit().getNom() : "Product #" + ligne.getProduitId();
        VBox info = new VBox(3);
        Label name = new Label(productName);
        name.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1f2933;");
        Label price = new Label("$" + ligne.getPrixUnitaire() + " x " + ligne.getQuantite() + " = $" + ligne.getSousTotal());
        price.setStyle("-fx-font-size: 12; -fx-text-fill: #6b7280;");
        info.getChildren().addAll(name, price);
        header.getChildren().addAll(emoji, info);
        HBox.setHgrow(info, Priority.ALWAYS);

        Spinner<Integer> qtySpinner = new Spinner<>();
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, ligne.getQuantite()));
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(70);

        Button updateBtn = new Button("Update");
        updateBtn.setStyle("-fx-padding: 4 12; -fx-font-weight: bold; -fx-font-size: 11;");
        updateBtn.setOnAction(e -> {
            try {
                int qty = Integer.parseInt(qtySpinner.getEditor().getText().trim());
                panierService.updateQuantite(ligne.getPanierId(), ligne.getProduitId(), qty);
                loadSelectedPanier();
            } catch (RuntimeException ex) { showError("Error", ex.getMessage()); }
        });

        Button removeBtn = new Button("Remove");
        removeBtn.getStyleClass().add("danger-button");
        removeBtn.setStyle("-fx-padding: 4 12; -fx-font-size: 11;");
        removeBtn.setOnAction(e -> {
            try {
                panierService.removeProduitFromPanier(ligne.getPanierId(), ligne.getProduitId());
                loadSelectedPanier();
            } catch (RuntimeException ex) { showError("Error", ex.getMessage()); }
        });

        Button buyBtn = new Button("Buy");
        buyBtn.setStyle("-fx-padding: 4 16; -fx-font-weight: bold; -fx-font-size: 11; -fx-background-color: #059669; -fx-text-fill: white; -fx-background-radius: 4;");
        buyBtn.setOnAction(e -> showStripePaymentDialog(ligne, productName));

        HBox actions = new HBox(8, new Label("Qty:") {{
            setStyle("-fx-text-fill: #1f2933; -fx-font-size: 12;");
        }}, qtySpinner, updateBtn, removeBtn, buyBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(header, actions);
        return card;
    }

    private void clearSelectedPanier() {
        Panier p = panierComboBox.getValue();
        if (p == null) { showError("Error", "Select a cart first."); return; }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm"); a.setHeaderText("Clear cart?");
        if (a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                panierService.clearPanier(p.getId());
                loadSelectedPanier();
            } catch (RuntimeException e) { showError("Error", e.getMessage()); }
        }
    }

    private void showStripePaymentDialog(ProduitPanier ligne, String productName) {
        long totalCents = ligne.getSousTotal().multiply(java.math.BigDecimal.valueOf(100)).longValue();

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Pay with Card");

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #ffffff;");

        Label title = new Label("Payment — " + productName);
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1f2933;");

        Label totalLabel = new Label("Total: $" + ligne.getSousTotal());
        totalLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #059669; -fx-font-weight: bold;");

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);

        TextField cardField = new TextField();
        cardField.setPromptText("4242 4242 4242 4242");

        TextField expMonthField = new TextField();
        expMonthField.setPromptText("12");
        expMonthField.setPrefWidth(60);

        TextField expYearField = new TextField();
        expYearField.setPromptText("2030");
        expYearField.setPrefWidth(60);

        TextField cvcField = new TextField();
        cvcField.setPromptText("123");
        cvcField.setPrefWidth(60);

        int r = 0;
        form.add(new Label("Card Number:"), 0, r); form.add(cardField, 1, r++);
        form.add(new Label("Exp Month:"), 0, r); form.add(expMonthField, 1, r++);
        form.add(new Label("Exp Year:"), 0, r); form.add(expYearField, 1, r++);
        form.add(new Label("CVC:"), 0, r); form.add(cvcField, 1, r++);

        Label hint = new Label("Use test card 4242 4242 4242 4242");
        hint.setStyle("-fx-font-size: 11; -fx-text-fill: #6b7280;");

        Button payBtn = new Button("Pay $" + ligne.getSousTotal());
        payBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 6;");
        payBtn.setMaxWidth(Double.MAX_VALUE);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);

        HBox btnRow = new HBox(10, payBtn, cancelBtn);

        payBtn.setOnAction(ev -> {
            try {
                int expMonth = Integer.parseInt(expMonthField.getText().trim());
                int expYear = Integer.parseInt(expYearField.getText().trim());
                String cardNum = cardField.getText().trim().replaceAll("\\s+", "");
                String cvc = cvcField.getText().trim();

                payBtn.setDisable(true);
                payBtn.setText("Processing...");

                com.stripe.model.PaymentIntent intent = stripeService.charge(
                        totalCents, "eur", cardNum, expMonth, expYear, cvc
                );

                if ("succeeded".equals(intent.getStatus())) {
                    panierService.removeProduitFromPanier(ligne.getPanierId(), ligne.getProduitId());
                    loadSelectedPanier();
                    dialog.close();
                    showInfo("Payment Successful!", productName + " purchased for $" + ligne.getSousTotal());
                } else {
                    showError("Payment Failed", "Status: " + intent.getStatus());
                }
            } catch (NumberFormatException ex) {
                showError("Validation", "Check expiry month/year and CVC.");
            } catch (Exception ex) {
                showError("Payment Error", ex.getMessage());
            }
        });

        cancelBtn.setOnAction(ev -> dialog.close());

        root.getChildren().addAll(title, totalLabel, form, hint, btnRow);
        Scene scene = new Scene(root, 360, 320);
        String css = getClass().getResource("/marketplace.css").toExternalForm();
        if (css != null) scene.getStylesheets().add(css);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t); a.setContentText(m); a.showAndWait();
    }

    private void showInfo(String t, String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(t); a.setContentText(m); a.showAndWait();
    }
}
