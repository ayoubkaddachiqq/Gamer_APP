package tn.esprit.Controller;

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
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entities.Panier;
import tn.esprit.entities.Produit;
import tn.esprit.entities.ProduitPanier;
import tn.esprit.entities.User;
import tn.esprit.entities.UserRole;
import tn.esprit.services.PanierService;
import tn.esprit.services.ProduitService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class MarketplaceController {

    @FXML private GridPane productsGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private Button refreshButton;
    @FXML private VBox panierItemsContainer;
    @FXML private ComboBox<Panier> panierComboBox;
    @FXML private TextField referenceField;
    @FXML private Label panierTotalLabel;
    @FXML private Button createPanierButton;
    @FXML private Button clearPanierButton;
    @FXML private Button adminButton;

    private final ProduitService produitService = new ProduitService();
    private final PanierService panierService = new PanierService();

    private final ObservableList<Produit> allProduits = FXCollections.observableArrayList();
    private final ObservableList<Produit> filteredProduits = FXCollections.observableArrayList();
    private final ObservableList<Panier> paniers = FXCollections.observableArrayList();
    private final ObservableList<ProduitPanier> lignesPanier = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        User currentUser = SessionManager.getCurrentUser();
        if (adminButton != null) {
            adminButton.setVisible(currentUser != null && currentUser.getRole() == UserRole.ADMIN);
            adminButton.setManaged(currentUser != null && currentUser.getRole() == UserRole.ADMIN);
        }
        setupSortOptions();
        setupEventHandlers();
        loadData();
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
        searchField.textProperty().addListener((o, old, v) -> applyFilters());
        categoryComboBox.setOnAction(e -> applyFilters());
        sortComboBox.setOnAction(e -> applyFilters());
        createPanierButton.setOnAction(e -> creerPanier());
        panierComboBox.setOnAction(e -> chargerPanierSelectionne());
        clearPanierButton.setOnAction(e -> viderPanier());
    }

    private void loadData() {
        try {
            allProduits.setAll(produitService.getProduitsActifs());
            mettreAJourFiltreCategorie();
            User user = SessionManager.getCurrentUser();
            if (user != null) {
                List<Panier> loaded = panierService.getPaniersByUser(user.getId());
                paniers.setAll(loaded);
                panierComboBox.setItems(paniers);
                if (!loaded.isEmpty()) {
                    panierComboBox.getSelectionModel().selectFirst();
                    chargerPanierSelectionne();
                }
            }
            applyFilters();
        } catch (RuntimeException e) {
            showError("Error", e.getMessage());
        }
    }

    private void mettreAJourFiltreCategorie() {
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
        if ("Stock (High-Low)".equals(s)) return java.util.Comparator.comparingInt(Produit::getStock).reversed();
        return (a, b) -> a.getNom().compareTo(b.getNom());
    }

    private String nul(String v) { return v == null ? "" : v; }

    private void displayCards() {
        productsGrid.getChildren().clear();
        int col = 0, row = 0;
        for (Produit p : filteredProduits) {
            productsGrid.add(creerCarte(p), col, row);
            if (++col == 2) { col = 0; row++; }
        }
    }

    private VBox creerCarte(Produit p) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: rgba(15, 23, 42, 0.9); -fx-border-color: rgba(216, 180, 254, 0.18); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 16; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 4);");
        card.setPrefHeight(200);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: rgba(25, 33, 55, 0.95); -fx-border-color: rgba(168, 85, 247, 0.4); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 16; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(168,85,247,0.2), 12, 0, 0, 6);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: rgba(15, 23, 42, 0.9); -fx-border-color: rgba(216, 180, 254, 0.18); -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 16; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 4);"));

        Label emoji = new Label("\uD83D\uDCE6");
        emoji.setStyle("-fx-font-size: 38; -fx-text-fill: #d8b4fe;");
        emoji.setMaxWidth(Double.MAX_VALUE);
        emoji.setAlignment(Pos.CENTER);

        Label name = new Label(p.getNom());
        name.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #f1f5f9; -fx-wrap-text: true;");
        name.setWrapText(true);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setAlignment(Pos.CENTER);

        Label price = new Label("$" + (p.getPrix() == null ? "0.00" : p.getPrix().toPlainString()));
        price.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #c084fc;");
        price.setMaxWidth(Double.MAX_VALUE);
        price.setAlignment(Pos.CENTER);

        String sc = p.getStock() <= 5 ? "#f87171" : "#4ade80";
        Label stock = new Label("Stock: " + p.getStock());
        stock.setStyle("-fx-font-size: 12; -fx-text-fill: " + sc + ";");
        stock.setMaxWidth(Double.MAX_VALUE);
        stock.setAlignment(Pos.CENTER);

        card.setOnMouseClicked(e -> showDetailDialog(p));
        card.getChildren().addAll(emoji, name, price, stock);
        return card;
    }

    private void showDetailDialog(Produit produit) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(produit.getNom());

        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #0f172a; -fx-border-color: rgba(216, 180, 254, 0.25); -fx-border-radius: 10; -fx-background-radius: 10;");

        Label emoji = new Label("\uD83D\uDCE6");
        emoji.setStyle("-fx-font-size: 56; -fx-text-fill: #d8b4fe;");
        emoji.setMaxWidth(Double.MAX_VALUE);
        emoji.setAlignment(Pos.CENTER);

        Label name = new Label(produit.getNom());
        name.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
        name.setMaxWidth(Double.MAX_VALUE);
        name.setAlignment(Pos.CENTER);

        GridPane details = new GridPane();
        details.setHgap(15);
        details.setVgap(8);
        details.setPadding(new Insets(10, 0, 10, 0));

        int r = 0;
        addDetailRow(details, r++, "Description", nul(produit.getDescription()));
        addDetailRow(details, r++, "Price", "$" + (produit.getPrix() == null ? "0.00" : produit.getPrix().toPlainString()));
        addDetailRow(details, r++, "Stock", String.valueOf(produit.getStock()));
        addDetailRow(details, r++, "Category", nul(produit.getCategorie()));

        Label qtyLbl = new Label("Quantity:");
        qtyLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #cbd5e1;");
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
        addBtn.setStyle("-fx-padding: 12 30; -fx-font-weight: bold; -fx-font-size: 14; -fx-background-color: linear-gradient(to right, #7c3aed, #a855f7); -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");
        addBtn.setPrefWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> {
            Panier panier = cartCombo.getValue();
            if (panier == null) { showError("Error", "Select a cart first."); return; }
            try {
                int qty = Integer.parseInt(qtySpinner.getEditor().getText().trim());
                panierService.ajouterProduit(panier.getId(), produit.getId(), qty);
                chargerPanierSelectionne();
                dialog.close();
                showInfo("Success", "Product added to cart!");
            } catch (RuntimeException ex) { showError("Error", ex.getMessage()); }
        });

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-padding: 12 30; -fx-font-size: 14; -fx-background-color: rgba(148, 163, 184, 0.15); -fx-text-fill: #e2e8f0; -fx-background-radius: 8; -fx-cursor: hand;");
        closeBtn.setPrefWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(emoji, name, details, qtyRow, cartCombo, addBtn, closeBtn);
        Scene dialogScene = new Scene(root, 380, 480);
        dialog.setScene(dialogScene);
        dialog.showAndWait();
    }

    private void addDetailRow(GridPane g, int row, String label, String value) {
        Label l = new Label(label + ":");
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #94a3b8;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 13; -fx-text-fill: #e2e8f0;");
        v.setWrapText(true);
        g.add(l, 0, row);
        g.add(v, 1, row);
    }

    // ─── PANIER ───

    private void creerPanier() {
        try {
            User user = SessionManager.getCurrentUser();
            if (user == null) { showError("Error", "Not logged in"); return; }
            Panier panier = new Panier();
            panier.setReference(referenceField.getText().isBlank() ? null : referenceField.getText());
            panier.setUserId(user.getId());
            Panier created = panierService.creerPanier(panier);
            paniers.add(created);
            panierComboBox.getSelectionModel().select(created);
            referenceField.clear();
            showInfo("Success", "Cart created!");
        } catch (RuntimeException e) { showError("Error", e.getMessage()); }
    }

    private void chargerPanierSelectionne() {
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
            afficherLignesPanier();
        } catch (RuntimeException e) { showError("Error", e.getMessage()); }
    }

    private void afficherLignesPanier() {
        panierItemsContainer.getChildren().clear();
        if (lignesPanier.isEmpty()) {
            Label empty = new Label("Cart is empty");
            empty.setStyle("-fx-font-size: 14; -fx-text-fill: #64748b; -fx-padding: 20;");
            panierItemsContainer.getChildren().add(empty);
            return;
        }
        for (ProduitPanier ligne : lignesPanier) {
            panierItemsContainer.getChildren().add(creerCartePanier(ligne));
        }
    }

    private VBox creerCartePanier(ProduitPanier ligne) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: rgba(15, 23, 42, 0.8); -fx-border-color: rgba(216, 180, 254, 0.15); -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 12;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label emoji = new Label("\uD83D\uDCE6");
        emoji.setStyle("-fx-font-size: 28;");

        String productName = ligne.getProduit() != null ? ligne.getProduit().getNom() : "Product #" + ligne.getProduitId();
        VBox info = new VBox(3);
        Label name = new Label(productName);
        name.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #f1f5f9;");
        Label price = new Label("$" + ligne.getPrixUnitaire() + " x " + ligne.getQuantite() + " = $" + ligne.getSousTotal());
        price.setStyle("-fx-font-size: 12; -fx-text-fill: #94a3b8;");
        info.getChildren().addAll(name, price);
        header.getChildren().addAll(emoji, info);
        HBox.setHgrow(info, Priority.ALWAYS);

        Spinner<Integer> qtySpinner = new Spinner<>();
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, ligne.getQuantite()));
        qtySpinner.setEditable(true);
        qtySpinner.setPrefWidth(70);

        Button updateBtn = new Button("Update");
        updateBtn.setStyle("-fx-padding: 4 12; -fx-font-weight: bold; -fx-font-size: 11; -fx-background-color: #7c3aed; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand;");
        updateBtn.setOnAction(e -> {
            try {
                int qty = Integer.parseInt(qtySpinner.getEditor().getText().trim());
                panierService.mettreAJourQuantite(ligne.getPanierId(), ligne.getProduitId(), qty);
                chargerPanierSelectionne();
            } catch (RuntimeException ex) { showError("Error", ex.getMessage()); }
        });

        Button removeBtn = new Button("Remove");
        removeBtn.setStyle("-fx-padding: 4 12; -fx-font-size: 11; -fx-text-fill: white; -fx-background-color: #dc2626; -fx-background-radius: 4; -fx-cursor: hand;");
        removeBtn.setOnAction(e -> {
            try {
                panierService.retirerProduit(ligne.getPanierId(), ligne.getProduitId());
                chargerPanierSelectionne();
            } catch (RuntimeException ex) { showError("Error", ex.getMessage()); }
        });

        HBox actions = new HBox(8, new Label("Qty:") {{
            setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");
        }}, qtySpinner, updateBtn, removeBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(header, actions);
        return card;
    }

    private void viderPanier() {
        Panier p = panierComboBox.getValue();
        if (p == null) { showError("Error", "Select a cart first."); return; }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm"); a.setHeaderText("Clear cart?");
        if (a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                panierService.viderPanier(p.getId());
                chargerPanierSelectionne();
            } catch (RuntimeException e) { showError("Error", e.getMessage()); }
        }
    }

    // ─── NAVIGATION ───

    @FXML private void handleHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainInterface.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setMinWidth(1100); stage.setMinHeight(700);
            stage.setTitle("Team Hub - E-Sport Recruitment"); stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleMyPosts(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MyPosts.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setMinWidth(1100); stage.setMinHeight(700);
            stage.setTitle("My Posts - Team Hub"); stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleAnnonces(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Annonces.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Gestion des Annonces"); stage.show();
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
            stage.setTitle("Team Hub - Gestion des Evenements"); stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Profile.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Team Hub - Profile"); stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleMarketplace(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Marketplace.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Marketplace"); stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleAdmin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AdminDashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setMinWidth(1100); stage.setMinHeight(700);
            stage.setTitle("Admin Dashboard - Team Hub"); stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void handleLogout(ActionEvent event) throws IOException {
        SessionManager.logout();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root, 1000, 700));
        stage.setTitle("Team Hub - Login"); stage.show();
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
