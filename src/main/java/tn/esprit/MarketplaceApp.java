package tn.esprit;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.model.Panier;
import tn.esprit.model.Produit;
import tn.esprit.model.ProduitPanier;
import tn.esprit.service.PanierService;
import tn.esprit.service.ProduitService;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MarketplaceApp extends Application {

    private final ProduitService produitService = new ProduitService();
    private final PanierService panierService = new PanierService();

    private final ObservableList<Produit> allProduits = FXCollections.observableArrayList();
    private final ObservableList<Produit> produits = FXCollections.observableArrayList();
    private final ObservableList<Panier> paniers = FXCollections.observableArrayList();
    private final ObservableList<ProduitPanier> lignesPanier = FXCollections.observableArrayList();

    private TableView<Produit> produitTable;
    private TableView<ProduitPanier> panierTable;
    private ComboBox<Panier> panierComboBox;
    private TreeView<String> categorieTree;
    private TextField searchField;
    private ComboBox<String> sortComboBox;
    private Label panierTotalLabel;

    private TextField nomField;
    private TextArea descriptionArea;
    private TextField prixField;
    private TextField stockField;
    private TextField categorieField;
    private CheckBox actifCheckBox;
    private TextField referenceField;
    private Spinner<Integer> quantiteSpinner;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");

        Label title = new Label("Marketplace");
        title.getStyleClass().add("app-title");

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> refreshAll());

        HBox header = new HBox(12, title, refreshButton);
        header.getStyleClass().add("app-header");
        HBox.setHgrow(title, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane(createProductPane(), createPanierPane());
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.52);

        root.setTop(header);
        root.setCenter(splitPane);

        Scene scene = new Scene(root, 1180, 720);
        scene.getStylesheets().add(getClass().getResource("/marketplace.css").toExternalForm());

        stage.setTitle("Marketplace Management");
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.show();

        refreshAll();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private VBox createProductPane() {
        searchField = new TextField();
        searchField.setPromptText("Search by name, category, or description");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyProductFilters());

        sortComboBox = new ComboBox<>();
        sortComboBox.getItems().addAll("Name A-Z", "Name Z-A", "Price Low-High", "Price High-Low", "Stock Low-High", "Stock High-Low");
        sortComboBox.getSelectionModel().selectFirst();
        sortComboBox.setOnAction(event -> applyProductFilters());

        HBox searchBar = new HBox(8, searchField, sortComboBox);
        searchBar.getStyleClass().add("action-row");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        categorieTree = new TreeView<>();
        categorieTree.setShowRoot(true);
        categorieTree.setPrefWidth(180);
        categorieTree.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> applyProductFilters()
        );

        produitTable = new TableView<>();
        produitTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        produitTable.getColumns().addAll(
                column("ID", "id", 60),
                column("Nom", "nom", 150),
                column("Prix", "prix", 90),
                column("Stock", "stock", 80),
                column("Categorie", "categorie", 120),
                column("Actif", "actif", 70)
        );
        produitTable.setItems(produits);
        produitTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, produit) -> fillProduitForm(produit)
        );

        HBox browser = new HBox(10, categorieTree, produitTable);
        HBox.setHgrow(produitTable, Priority.ALWAYS);
        VBox.setVgrow(browser, Priority.ALWAYS);

        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        form.setHgap(10);
        form.setVgap(8);

        nomField = new TextField();
        descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(3);
        prixField = new TextField();
        stockField = new TextField();
        categorieField = new TextField();
        actifCheckBox = new CheckBox("Active");
        actifCheckBox.setSelected(true);

        addFormRow(form, 0, "Nom", nomField);
        addFormRow(form, 1, "Description", descriptionArea);
        addFormRow(form, 2, "Prix", prixField);
        addFormRow(form, 3, "Stock", stockField);
        addFormRow(form, 4, "Categorie", categorieField);
        form.add(actifCheckBox, 1, 5);

        Button createButton = new Button("Create");
        createButton.setOnAction(event -> createProduit());
        Button updateButton = new Button("Update");
        updateButton.setOnAction(event -> updateProduit());
        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("danger-button");
        deleteButton.setOnAction(event -> deleteProduit());
        Button clearButton = new Button("Clear");
        clearButton.setOnAction(event -> clearProduitForm());

        HBox actions = new HBox(8, createButton, updateButton, deleteButton, clearButton);
        actions.getStyleClass().add("action-row");

        VBox pane = new VBox(12, sectionTitle("Products"), searchBar, browser, form, actions);
        pane.getStyleClass().add("section");
        return pane;
    }

    private VBox createPanierPane() {
        panierComboBox = new ComboBox<>(paniers);
        panierComboBox.setMaxWidth(Double.MAX_VALUE);
        panierComboBox.setPromptText("Select panier");
        panierComboBox.setCellFactory(listView -> new PanierListCell());
        panierComboBox.setButtonCell(new PanierListCell());
        panierComboBox.setOnAction(event -> loadSelectedPanier());

        referenceField = new TextField();
        referenceField.setPromptText("Reference for new panier");

        Button createPanierButton = new Button("Create Panier");
        createPanierButton.setOnAction(event -> createPanier());

        HBox panierHeader = new HBox(8, panierComboBox, referenceField, createPanierButton);
        panierHeader.getStyleClass().add("action-row");
        HBox.setHgrow(panierComboBox, Priority.ALWAYS);
        HBox.setHgrow(referenceField, Priority.ALWAYS);

        quantiteSpinner = new Spinner<>();
        quantiteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        quantiteSpinner.setEditable(true);

        Button addButton = new Button("Add Selected Product");
        addButton.setOnAction(event -> addSelectedProduitToPanier());
        Button updateQuantityButton = new Button("Update Quantity");
        updateQuantityButton.setOnAction(event -> updateSelectedLineQuantity());
        Button removeButton = new Button("Remove Line");
        removeButton.getStyleClass().add("danger-button");
        removeButton.setOnAction(event -> removeSelectedLine());
        Button clearButton = new Button("Clear Panier");
        clearButton.getStyleClass().add("danger-button");
        clearButton.setOnAction(event -> clearSelectedPanier());

        HBox panierActions = new HBox(8, new Label("Qty"), quantiteSpinner, addButton, updateQuantityButton, removeButton, clearButton);
        panierActions.getStyleClass().add("action-row");

        panierTable = new TableView<>();
        panierTable.getColumns().addAll(
                lineColumn("Produit", 170),
                lineNumberColumn("Quantite", "quantite", 90),
                lineNumberColumn("Prix", "prixUnitaire", 90),
                lineNumberColumn("Sous-total", "sousTotal", 100)
        );
        panierTable.setItems(lignesPanier);
        panierTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, line) -> {
            if (line != null) {
                quantiteSpinner.getValueFactory().setValue(line.getQuantite());
            }
        });

        panierTotalLabel = new Label("Total: 0.00");
        panierTotalLabel.getStyleClass().add("total-label");

        VBox pane = new VBox(12, sectionTitle("Panier"), panierHeader, panierActions, panierTable, panierTotalLabel);
        pane.getStyleClass().add("section");
        VBox.setVgrow(panierTable, Priority.ALWAYS);
        return pane;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private <S, T> TableColumn<S, T> column(String title, String property, int width) {
        TableColumn<S, T> column = new TableColumn<>(title);
        column.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        return column;
    }

    private TableColumn<ProduitPanier, String> lineColumn(String title, int width) {
        TableColumn<ProduitPanier, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> {
            Produit produit = data.getValue().getProduit();
            return new ReadOnlyStringWrapper(produit == null ? String.valueOf(data.getValue().getProduitId()) : produit.getNom());
        });
        column.setPrefWidth(width);
        return column;
    }

    private <T> TableColumn<ProduitPanier, T> lineNumberColumn(String title, String property, int width) {
        TableColumn<ProduitPanier, T> column = new TableColumn<>(title);
        column.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(property));
        column.setPrefWidth(width);
        return column;
    }

    private void addFormRow(GridPane form, int row, String labelText, javafx.scene.Node field) {
        Label label = new Label(labelText);
        form.add(label, 0, row);
        form.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private void refreshAll() {
        try {
            Integer selectedPanierId = panierComboBox.getSelectionModel().getSelectedItem() == null
                    ? null
                    : panierComboBox.getSelectionModel().getSelectedItem().getId();

            allProduits.setAll(produitService.listProduits());
            rebuildCategorieTree();
            applyProductFilters();
            List<Panier> loadedPaniers = panierService.listPaniers();
            paniers.setAll(loadedPaniers);

            if (selectedPanierId != null) {
                selectPanierById(selectedPanierId);
            } else if (!loadedPaniers.isEmpty()) {
                panierComboBox.getSelectionModel().selectFirst();
            }
            loadSelectedPanier();
        } catch (RuntimeException exception) {
            showError("Unable to refresh data", exception);
        }
    }

    private void rebuildCategorieTree() {
        if (categorieTree == null) {
            return;
        }

        String selectedValue = categorieTree.getSelectionModel().getSelectedItem() == null
                ? "All"
                : categorieTree.getSelectionModel().getSelectedItem().getValue();

        TreeItem<String> root = new TreeItem<>("All");
        root.setExpanded(true);
        allProduits.stream()
                .map(Produit::getCategorie)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .map(TreeItem::new)
                .forEach(root.getChildren()::add);

        categorieTree.setRoot(root);
        root.getChildren().stream()
                .filter(item -> item.getValue().equals(selectedValue))
                .findFirst()
                .ifPresentOrElse(
                        item -> categorieTree.getSelectionModel().select(item),
                        () -> categorieTree.getSelectionModel().select(root)
                );
    }

    private void applyProductFilters() {
        if (searchField == null || sortComboBox == null) {
            produits.setAll(allProduits);
            return;
        }

        String search = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String selectedCategory = categorieTree == null || categorieTree.getSelectionModel().getSelectedItem() == null
                ? "All"
                : categorieTree.getSelectionModel().getSelectedItem().getValue();

        List<Produit> filtered = allProduits.stream()
                .filter(produit -> selectedCategory.equals("All")
                        || selectedCategory.equals(nullToEmpty(produit.getCategorie()).trim()))
                .filter(produit -> search.isEmpty()
                        || containsIgnoreCase(produit.getNom(), search)
                        || containsIgnoreCase(produit.getCategorie(), search)
                        || containsIgnoreCase(produit.getDescription(), search))
                .sorted(productComparator())
                .toList();

        produits.setAll(filtered);
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private Comparator<Produit> productComparator() {
        String sort = sortComboBox.getSelectionModel().getSelectedItem();
        if ("Name Z-A".equals(sort)) {
            return Comparator.comparing((Produit produit) -> nullToEmpty(produit.getNom()), String.CASE_INSENSITIVE_ORDER).reversed();
        }
        if ("Price Low-High".equals(sort)) {
            return Comparator.comparing(Produit::getPrix, Comparator.nullsLast(BigDecimal::compareTo));
        }
        if ("Price High-Low".equals(sort)) {
            return Comparator.comparing(Produit::getPrix, Comparator.nullsLast(BigDecimal::compareTo)).reversed();
        }
        if ("Stock Low-High".equals(sort)) {
            return Comparator.comparingInt(Produit::getStock);
        }
        if ("Stock High-Low".equals(sort)) {
            return Comparator.comparingInt(Produit::getStock).reversed();
        }
        return Comparator.comparing((Produit produit) -> nullToEmpty(produit.getNom()), String.CASE_INSENSITIVE_ORDER);
    }

    private void createProduit() {
        try {
            Produit produit = readProduitForm(new Produit());
            produitService.addProduit(produit);
            refreshAll();
            clearProduitForm();
        } catch (RuntimeException exception) {
            showError("Unable to create produit", exception);
        }
    }

    private void updateProduit() {
        Produit selected = produitTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Select a product first.");
            return;
        }
        try {
            Produit produit = readProduitForm(selected);
            produitService.updateProduit(produit);
            refreshAll();
        } catch (RuntimeException exception) {
            showError("Unable to update produit", exception);
        }
    }

    private void deleteProduit() {
        Produit selected = produitTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Select a product first.");
            return;
        }
        if (!confirm("Delete selected product?")) {
            return;
        }
        try {
            produitService.deleteProduit(selected.getId());
            refreshAll();
            clearProduitForm();
        } catch (RuntimeException exception) {
            showError("Unable to delete produit", exception);
        }
    }

    private Produit readProduitForm(Produit produit) {
        produit.setNom(nomField.getText().trim());
        produit.setDescription(descriptionArea.getText().trim());
        produit.setPrix(new BigDecimal(prixField.getText().trim()));
        produit.setStock(Integer.parseInt(stockField.getText().trim()));
        produit.setCategorie(categorieField.getText().trim());
        produit.setActif(actifCheckBox.isSelected());
        return produit;
    }

    private void fillProduitForm(Produit produit) {
        if (produit == null) {
            clearProduitForm();
            return;
        }
        nomField.setText(nullToEmpty(produit.getNom()));
        descriptionArea.setText(nullToEmpty(produit.getDescription()));
        prixField.setText(produit.getPrix() == null ? "" : produit.getPrix().toPlainString());
        stockField.setText(String.valueOf(produit.getStock()));
        categorieField.setText(nullToEmpty(produit.getCategorie()));
        actifCheckBox.setSelected(produit.isActif());
    }

    private void clearProduitForm() {
        produitTable.getSelectionModel().clearSelection();
        nomField.clear();
        descriptionArea.clear();
        prixField.clear();
        stockField.clear();
        categorieField.clear();
        actifCheckBox.setSelected(true);
    }

    private void createPanier() {
        try {
            Panier panier = new Panier();
            panier.setReference(referenceField.getText().trim().isEmpty() ? null : referenceField.getText().trim());
            Panier created = panierService.createPanier(panier);
            referenceField.clear();
            refreshAll();
            panierComboBox.getSelectionModel().select(created);
        } catch (RuntimeException exception) {
            showError("Unable to create panier", exception);
        }
    }

    private void addSelectedProduitToPanier() {
        Panier panier = panierComboBox.getSelectionModel().getSelectedItem();
        Produit produit = produitTable.getSelectionModel().getSelectedItem();
        if (panier == null || produit == null) {
            showInfo("Select a panier and a product first.");
            return;
        }
        try {
            int quantite = readQuantiteSpinner();
            panierService.addProduitToPanier(panier.getId(), produit.getId(), quantite);
            refreshAll();
            selectPanierById(panier.getId());
        } catch (RuntimeException exception) {
            showError("Unable to add product to panier", exception);
        }
    }

    private void updateSelectedLineQuantity() {
        Panier panier = panierComboBox.getSelectionModel().getSelectedItem();
        ProduitPanier line = panierTable.getSelectionModel().getSelectedItem();
        if (panier == null || line == null) {
            showInfo("Select a panier line first.");
            return;
        }
        try {
            int quantite = readQuantiteSpinner();
            panierService.updateQuantite(panier.getId(), line.getProduitId(), quantite);
            refreshAll();
            selectPanierById(panier.getId());
        } catch (RuntimeException exception) {
            showError("Unable to update quantity", exception);
        }
    }

    private void removeSelectedLine() {
        Panier panier = panierComboBox.getSelectionModel().getSelectedItem();
        ProduitPanier line = panierTable.getSelectionModel().getSelectedItem();
        if (panier == null || line == null) {
            showInfo("Select a panier line first.");
            return;
        }
        try {
            panierService.removeProduitFromPanier(panier.getId(), line.getProduitId());
            refreshAll();
            selectPanierById(panier.getId());
        } catch (RuntimeException exception) {
            showError("Unable to remove line", exception);
        }
    }

    private void clearSelectedPanier() {
        Panier panier = panierComboBox.getSelectionModel().getSelectedItem();
        if (panier == null) {
            showInfo("Select a panier first.");
            return;
        }
        if (!confirm("Clear selected panier?")) {
            return;
        }
        try {
            panierService.clearPanier(panier.getId());
            refreshAll();
            selectPanierById(panier.getId());
        } catch (RuntimeException exception) {
            showError("Unable to clear panier", exception);
        }
    }

    private void loadSelectedPanier() {
        Panier selected = panierComboBox == null ? null : panierComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) {
            lignesPanier.clear();
            if (panierTotalLabel != null) {
                panierTotalLabel.setText("Total: 0.00");
            }
            return;
        }
        try {
            Panier details = panierService.getPanierDetails(selected.getId());
            lignesPanier.setAll(details.getLignes());
            panierTotalLabel.setText("Total: " + details.getTotal());
        } catch (RuntimeException exception) {
            showError("Unable to load panier", exception);
        }
    }

    private void selectPanierById(Integer panierId) {
        paniers.stream()
                .filter(panier -> panier.getId().equals(panierId))
                .findFirst()
                .ifPresent(panier -> panierComboBox.getSelectionModel().select(panier));
    }

    private int readQuantiteSpinner() {
        String editorValue = quantiteSpinner.getEditor().getText();
        int quantite = Integer.parseInt(editorValue.trim());
        quantiteSpinner.getValueFactory().setValue(quantite);
        return quantite;
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm");
        alert.setHeaderText(message);
        alert.getButtonTypes().setAll(ButtonType.CANCEL, new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE));
        return alert.showAndWait()
                .filter(buttonType -> buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                .isPresent();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Marketplace");
        alert.setHeaderText(message);
        alert.showAndWait();
    }

    private void showError(String header, RuntimeException exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Operation failed");
        alert.setHeaderText(header);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static class PanierListCell extends ListCell<Panier> {

        @Override
        protected void updateItem(Panier panier, boolean empty) {
            super.updateItem(panier, empty);
            if (empty || panier == null) {
                setText(null);
                return;
            }
            setText(panier.getReference() + "  #" + panier.getId());
        }
    }
}
