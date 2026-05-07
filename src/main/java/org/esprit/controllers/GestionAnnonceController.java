package org.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.esprit.models.Annonce;
import org.esprit.services.AnnonceService;
import org.esprit.services.CategorieService;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class GestionAnnonceController {

    @FXML private ListView<Annonce> listAnnonces;
    @FXML private TextField tfRecherche;
    @FXML private TextField tfSalaireMin;
    @FXML private TextField tfSalaireMax;

    private final AnnonceService annonceService = new AnnonceService();
    private final CategorieService categorieService = new CategorieService();
    private final ObservableList<Annonce> annonces = FXCollections.observableArrayList();
    private final FilteredList<Annonce> annoncesFiltrees = new FilteredList<>(annonces, annonce -> true);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    @FXML
    private void initialize() {
        listAnnonces.setItems(annoncesFiltrees);
        listAnnonces.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Annonce annonce, boolean empty) {
                super.updateItem(annonce, empty);
                setText(null);
                setGraphic(empty || annonce == null ? null : creerCarteAnnonce(annonce));
            }
        });

        tfRecherche.textProperty().addListener((observable, oldValue, newValue) -> appliquerFiltres());
        tfSalaireMin.textProperty().addListener((observable, oldValue, newValue) -> appliquerFiltres());
        tfSalaireMax.textProperty().addListener((observable, oldValue, newValue) -> appliquerFiltres());
        chargerAnnonces();
    }

    private HBox creerCarteAnnonce(Annonce annonce) {
        HBox carte = new HBox(18);
        carte.setAlignment(Pos.CENTER_LEFT);
        carte.getStyleClass().add("annonce-card");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(120);
        imageView.setFitHeight(78);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("annonce-card-image");
        afficherImage(imageView, annonce.getImagePath());

        VBox contenu = new VBox(8);
        contenu.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(contenu, Priority.ALWAYS);

        Label titre = new Label(valeurOuVide(annonce.getTitre()));
        titre.getStyleClass().add("annonce-card-title");
        titre.setWrapText(true);

        Label description = new Label(valeurOuVide(annonce.getDescription()));
        description.getStyleClass().add("annonce-card-description");
        description.setWrapText(true);
        description.setMaxWidth(Double.MAX_VALUE);

        HBox details = new HBox(10);
        details.setAlignment(Pos.CENTER_LEFT);
        details.getChildren().addAll(
                creerBadge("Jeu", annonce.getJeu()),
                creerBadge("Salaire", String.format("%.2f DT", annonce.getSalaire())),
                creerBadge("Categorie", annonce.getNomCategorie()),
                creerBadge("Statut", annonce.getStatut()),
                creerBadge("Publication", formaterDatePublication(annonce))
        );

        contenu.getChildren().addAll(titre, description, details);

        VBox actions = new VBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setMinWidth(120);

        Button btnModifier = new Button("Modifier");
        Button btnSupprimer = new Button("Supprimer");
        btnModifier.setMaxWidth(Double.MAX_VALUE);
        btnSupprimer.setMaxWidth(Double.MAX_VALUE);
        btnModifier.getStyleClass().add("btn-secondary");
        btnSupprimer.getStyleClass().add("btn-danger");

        btnModifier.setOnAction(event -> {
            AjouterAnnonceController.annonceToEdit = annonce;
            ouvrirFormulaire();
        });
        btnSupprimer.setOnAction(event -> supprimerAnnonce(annonce));

        actions.getChildren().addAll(btnModifier, btnSupprimer);
        carte.getChildren().addAll(imageView, contenu, actions);
        return carte;
    }

    private VBox creerBadge(String libelle, String valeur) {
        VBox badge = new VBox(2);
        badge.getStyleClass().add("annonce-badge");

        Label titre = new Label(libelle);
        titre.getStyleClass().add("annonce-badge-label");

        Label contenu = new Label(valeurOuVide(valeur));
        contenu.getStyleClass().add("annonce-badge-value");
        contenu.setWrapText(true);

        badge.getChildren().addAll(titre, contenu);
        return badge;
    }

    private void afficherImage(ImageView imageView, String imagePath) {
        imageView.setImage(null);

        if (imagePath == null || imagePath.trim().isEmpty()) {
            return;
        }

        File file = new File(imagePath);
        if (file.exists()) {
            imageView.setImage(new Image(file.toURI().toString(), 120, 78, true, true));
            return;
        }

        URL resource = getClass().getResource(imagePath);
        if (resource != null) {
            imageView.setImage(new Image(resource.toExternalForm(), 120, 78, true, true));
        }
    }

    @FXML
    private void ajouterAnnonce() {
        AjouterAnnonceController.annonceToEdit = null;
        ouvrirFormulaire();
    }

    @FXML
    private void rechercherAnnonce() {
        appliquerFiltres();
    }

    private void supprimerAnnonce(Annonce annonce) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer l'annonce");
        alert.setContentText("Voulez-vous vraiment supprimer : " + annonce.getTitre() + " ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                annonceService.supprimer(annonce.getId());
                chargerAnnonces();
                afficherInformation("Annonce supprimée avec succès !");
            } catch (Exception e) {
                afficherErreur(e.getMessage());
            }
        }
    }

    private void chargerAnnonces() {
        try {
            categorieService.assurerCategoriesParDefaut();
            annonceService.assurerAnnoncesParDefaut();
            List<Annonce> annoncesChargees = annonceService.getAll();
            annonces.setAll(annoncesChargees);
            appliquerFiltres();
        } catch (Exception e) {
            afficherErreur(e.getMessage());
            e.printStackTrace();
        }
    }

    private void appliquerFiltres() {
        String filtre = normaliser(tfRecherche.getText());
        Double salaireMin = lireSalaire(tfSalaireMin.getText());
        Double salaireMax = lireSalaire(tfSalaireMax.getText());

        annoncesFiltrees.setPredicate(annonce -> {
            boolean correspondTexte = filtre.isEmpty()
                    || contient(annonce.getTitre(), filtre)
                    || contient(annonce.getDescription(), filtre)
                    || contient(annonce.getJeu(), filtre)
                    || contient(annonce.getNomCategorie(), filtre)
                    || contient(annonce.getStatut(), filtre)
                    || contient(formaterDatePublication(annonce), filtre);

            boolean correspondSalaireMin = salaireMin == null || annonce.getSalaire() >= salaireMin;
            boolean correspondSalaireMax = salaireMax == null || annonce.getSalaire() <= salaireMax;

            return correspondTexte && correspondSalaireMin && correspondSalaireMax;
        });
    }

    private boolean contient(String valeur, String filtre) {
        return valeur != null && valeur.toLowerCase(Locale.ROOT).contains(filtre);
    }

    private String normaliser(String valeur) {
        return valeur == null ? "" : valeur.trim().toLowerCase(Locale.ROOT);
    }

    private Double lireSalaire(String valeur) {
        String texte = valeur == null ? "" : valeur.trim().replace(',', '.');
        if (texte.isEmpty()) {
            return null;
        }

        try {
            return Double.parseDouble(texte);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String valeurOuVide(String valeur) {
        return valeur == null ? "" : valeur;
    }

    private String formaterDatePublication(Annonce annonce) {
        return annonce.getDatePublication() == null ? "" : dateFormat.format(annonce.getDatePublication());
    }

    private void ouvrirFormulaire() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterAnnonce.fxml"));
            Parent root = loader.load();
            listAnnonces.getScene().setRoot(root);
        } catch (Exception e) {
            afficherErreur(e.getMessage());
        }
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void afficherInformation(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
