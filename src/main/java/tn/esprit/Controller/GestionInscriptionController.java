package tn.esprit.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Evenement;
import tn.esprit.entities.Inscription;
import tn.esprit.entities.TypeEvenement;
import tn.esprit.entities.User;
import tn.esprit.entities.UserRole;
import tn.esprit.services.EvenementService;
import tn.esprit.services.InscriptionService;
import tn.esprit.services.TypeEvenementService;
import tn.esprit.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class GestionInscriptionController {

    @FXML private FlowPane eventsFlow;
    @FXML private TextField tfRechercheTitre;
    @FXML private TextField tfRechercheLieu;
    @FXML private TextField tfUtilisateurId;
    @FXML private Label lbSelection;
    @FXML private Label lbEmptyState;
    @FXML private Button btnInscrire;
    @FXML private Button btnRetourEvenements;
    @FXML private Button btnRetourHeader;

    private final EvenementService evenementService = new EvenementService();
    private final InscriptionService inscriptionService = new InscriptionService();
    private final TypeEvenementService typeService = new TypeEvenementService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private Evenement evenementSelectionne;
    private List<TypeEvenement> typesEvenement;

    @FXML
    void initialize() {
        User currentUser = SessionManager.getCurrentUser();
        boolean isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;
        if (btnRetourEvenements != null) {
            btnRetourEvenements.setVisible(isAdmin);
            btnRetourEvenements.setManaged(isAdmin);
        }
        if (btnRetourHeader != null) {
            btnRetourHeader.setVisible(isAdmin);
            btnRetourHeader.setManaged(isAdmin);
        }
        chargerEvenements();
        afficherSelection();
    }

    private void chargerEvenements() {
        try {
            typesEvenement = typeService.getAll();
            List<Evenement> evenements = evenementService.getAll();
            afficherEvenements(evenements);
        } catch (Exception e) {
            afficherAlerte(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    private void afficherEvenements(List<Evenement> evenements) {
        eventsFlow.getChildren().clear();
        boolean empty = evenements == null || evenements.isEmpty();
        lbEmptyState.setVisible(empty);
        lbEmptyState.setManaged(empty);

        if (evenements != null) {
            for (Evenement evenement : evenements) {
                eventsFlow.getChildren().add(creerCarteEvenement(evenement));
            }
        }
    }

    @FXML
    void rechercher() {
        String titre = tfRechercheTitre.getText().trim();
        String lieu = tfRechercheLieu.getText().trim();
        try {
            List<Evenement> evenements;
            if (!lieu.isEmpty()) {
                evenements = evenementService.rechercherParLieu(lieu);
            } else if (!titre.isEmpty()) {
                evenements = evenementService.rechercherParTitre(titre);
            } else {
                evenements = evenementService.getAll();
            }
            afficherEvenements(evenements);
        } catch (Exception e) {
            afficherAlerte(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    @FXML
    void reinitialiser() {
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

        Label date = new Label(evenement.getDateDebut() == null ? "-" : dateFormat.format(evenement.getDateDebut()));
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
        if (chemin == null || chemin.isBlank()) return null;
        try {
            if (chemin.startsWith("/")) {
                javafx.scene.image.Image img = new Image(getClass().getResourceAsStream(chemin), 210, 118, true, true);
                return img.isError() ? null : img;
            }
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
        if (typesEvenement == null) return "Type inconnu";
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
    void inscrire() {
        if (evenementSelectionne == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Choisis d'abord un evenement.");
            return;
        }

        try {
            int utilisateurId = Integer.parseInt(tfUtilisateurId.getText().trim());
            User currentUser = SessionManager.getCurrentUser();
            if (currentUser != null && currentUser.getId() == utilisateurId) {
                if (inscriptionService.estInscrit(evenementSelectionne.getId(), utilisateurId)) {
                    afficherAlerte(Alert.AlertType.INFORMATION, "Vous etes deja inscrit a cet evenement");
                    return;
                }
                int nbInscrits = inscriptionService.compterInscriptions(evenementSelectionne.getId());
                if (nbInscrits >= evenementSelectionne.getNbParticipantsMax()) {
                    afficherAlerte(Alert.AlertType.ERROR, "Cet evenement est complet");
                    return;
                }
            }
            Inscription inscription = new Inscription(evenementSelectionne.getId(), utilisateurId, "En attente");
            inscriptionService.ajouter(inscription);
            afficherAlerte(Alert.AlertType.INFORMATION, "Inscription envoyee pour : " + evenementSelectionne.getTitre());
            tfUtilisateurId.clear();
        } catch (NumberFormatException e) {
            afficherAlerte(Alert.AlertType.ERROR, "L'ID utilisateur doit etre un nombre.");
        } catch (Exception e) {
            afficherAlerte(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    private void afficherAlerte(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    void retour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Evenements.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) eventsFlow.getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Gestion des Evenements");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            afficherAlerte(Alert.AlertType.ERROR, e.getMessage());
        }
    }
}
