package org.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.esprit.models.Evenement;
import org.esprit.models.Inscription;
import org.esprit.models.TypeEvenement;
import org.esprit.services.EvenementService;
import org.esprit.services.InscriptionService;
import org.esprit.services.TypeEvenementService;
import org.esprit.utils.UiEffects;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GestionInscriptionController {

    @FXML private Parent rootPane;
    @FXML private FlowPane eventsFlow;
    @FXML private TextField tfRechercheTitre;
    @FXML private TextField tfRechercheLieu;
    @FXML private TextField tfUtilisateurId;
    @FXML private Label lbSelection;
    @FXML private Label lbEmptyState;
    @FXML private Button btnInscrire;

    private final EvenementService evenementService = new EvenementService();
    private final InscriptionService inscriptionService = new InscriptionService();
    private final TypeEvenementService typeService = new TypeEvenementService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private Evenement evenementSelectionne;
    private List<TypeEvenement> typesEvenement;

    @FXML
    void initialize() {
        UiEffects.applyEntranceAndHover(rootPane);
        evenementSelectionne = GestionEvenementController.evenementSelectionne;
        chargerEvenements();
        afficherSelection();
    }

    private void chargerEvenements() {
        List<Evenement> evenements = evenementService.getAll();
        typesEvenement = typeService.getAll();
        afficherEvenements(evenements);
    }

    private void afficherEvenements(List<Evenement> evenements) {
        eventsFlow.getChildren().clear();
        lbEmptyState.setVisible(evenements.isEmpty());
        lbEmptyState.setManaged(evenements.isEmpty());

        for (Evenement evenement : evenements) {
            eventsFlow.getChildren().add(creerCarteEvenement(evenement));
        }
    }

    @FXML
    void rechercher(ActionEvent event) {
        String titre = tfRechercheTitre.getText().trim();
        String lieu = tfRechercheLieu.getText().trim();
        List<Evenement> evenements;

        if (!lieu.isEmpty()) {
            evenements = evenementService.rechercherParLieu(lieu);
        } else if (!titre.isEmpty()) {
            evenements = evenementService.rechercherParTitre(titre);
        } else {
            evenements = evenementService.getAll();
        }

        afficherEvenements(evenements);
    }

    @FXML
    void reinitialiser(ActionEvent event) {
        tfRechercheTitre.clear();
        tfRechercheLieu.clear();
        chargerEvenements();
    }

    private VBox creerCarteEvenement(Evenement evenement) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(210);
        imageView.setFitHeight(118);
        imageView.setPreserveRatio(false);

        StackPane imageBox = new StackPane();
        imageBox.setPrefSize(210, 118);
        imageBox.setMinSize(210, 118);
        imageBox.setMaxSize(210, 118);
        imageBox.getStyleClass().add("event-card-image");

        Image image = chargerImage(evenement.getImage());
        if (image != null) {
            imageView.setImage(image);
            imageBox.getChildren().add(imageView);
        } else {
            Label fallback = new Label("Aucune image");
            fallback.getStyleClass().add("event-image-placeholder");
            imageBox.getChildren().add(fallback);
        }

        Label titre = new Label(valeurOuTiret(evenement.getTitre()));
        titre.getStyleClass().add("event-card-title");
        titre.setWrapText(true);

        Label lieu = new Label(valeurOuTiret(evenement.getLieu()));
        lieu.getStyleClass().add("event-card-detail");
        lieu.setWrapText(true);

        Label date = new Label(evenement.getDateDebut() == null ? "-" : evenement.getDateDebut().format(dateFormatter));
        date.getStyleClass().add("event-card-detail");
        date.setWrapText(true);

        Label type = new Label(libelleType(evenement.getTypeId()));
        type.getStyleClass().add("status-pill");

        Label statut = new Label(valeurOuTiret(evenement.getStatut()));
        statut.getStyleClass().add("status-pill");

        Label capacite = new Label(evenement.getNbParticipantsMax() + " participants max");
        capacite.getStyleClass().add("event-card-detail");

        HBox badges = new HBox(8, type, statut);
        badges.setAlignment(Pos.CENTER_LEFT);

        Label hint = new Label("Cliquer pour choisir");
        hint.getStyleClass().add("event-card-hint");

        VBox details = new VBox(8, titre, badges, lieu, date, capacite, hint);
        details.getStyleClass().add("event-card-body");

        VBox card = new VBox(imageBox, details);
        card.getStyleClass().add("event-card");
        card.setPrefWidth(210);
        card.setMinWidth(210);
        card.setMaxWidth(210);
        card.setOnMouseClicked(event -> selectionner(evenement));
        return card;
    }

    private Image chargerImage(String chemin) {
        if (chemin == null || chemin.isBlank()) {
            return null;
        }
        try {
            String source = chemin.startsWith("http://") || chemin.startsWith("https://") || chemin.startsWith("file:")
                    ? chemin
                    : new File(chemin).toURI().toString();
            Image image = new Image(source, 210, 118, false, true, false);
            return image.isError() ? null : image;
        } catch (Exception e) {
            return null;
        }
    }

    private String libelleType(int typeId) {
        return typesEvenement.stream()
                .filter(type -> type.getId() == typeId)
                .map(TypeEvenement::getLibelle)
                .findFirst()
                .orElse("Type inconnu");
    }

    private String valeurOuTiret(String valeur) {
        return valeur == null || valeur.isBlank() ? "-" : valeur;
    }

    private void selectionner(Evenement evenement) {
        evenementSelectionne = evenement;
        GestionEvenementController.evenementSelectionne = evenement;
        afficherSelection();
        tfUtilisateurId.requestFocus();
    }

    private void afficherSelection() {
        boolean hasSelection = evenementSelectionne != null;
        btnInscrire.setDisable(!hasSelection);
        lbSelection.setText(hasSelection
                ? "Evenement choisi : " + evenementSelectionne.getTitre()
                : "Choisis un evenement dans la liste.");
    }

    @FXML
    void inscrire(ActionEvent event) {
        if (evenementSelectionne == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Choisis d'abord un evenement.");
            return;
        }

        try {
            int utilisateurId = Integer.parseInt(tfUtilisateurId.getText().trim());
            Inscription inscription = new Inscription(evenementSelectionne.getId(), utilisateurId, "En attente");
            inscriptionService.add(inscription);
            afficherAlerte(Alert.AlertType.INFORMATION, "Inscription envoyee pour : " + evenementSelectionne.getTitre());
            tfUtilisateurId.clear();
        } catch (NumberFormatException e) {
            afficherAlerte(Alert.AlertType.ERROR, "L'ID utilisateur doit etre un nombre.");
        }
    }

    private void afficherAlerte(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.show();
    }

    @FXML
    void retour(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GestionEvenement.fxml"));
            rootPane.getScene().setRoot(root);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
