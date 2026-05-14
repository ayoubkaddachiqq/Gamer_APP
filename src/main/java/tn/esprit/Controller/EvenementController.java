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
import tn.esprit.entities.TypeEvenement;
import tn.esprit.entities.User;
import tn.esprit.entities.UserRole;
import tn.esprit.services.EvenementService;
import tn.esprit.services.TypeEvenementService;
import tn.esprit.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

public class EvenementController {

    @FXML private FlowPane eventsFlow;
    @FXML private TextField tfRechercheTitre;
    @FXML private TextField tfRechercheLieu;
    @FXML private Label lbEmptyState;
    @FXML private Button btnAjouter;
    @FXML private Button btnAdminInscriptionsFooter;
    @FXML private Button adminButton;

    private final EvenementService evenementService = new EvenementService();
    private final TypeEvenementService typeService = new TypeEvenementService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private List<TypeEvenement> typesEvenement;

    @FXML
    private void initialize() {
        User currentUser = SessionManager.getCurrentUser();
        boolean isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;
        btnAjouter.setVisible(isAdmin);
        btnAjouter.setManaged(isAdmin);
        if (btnAdminInscriptionsFooter != null) {
            btnAdminInscriptionsFooter.setVisible(isAdmin);
            btnAdminInscriptionsFooter.setManaged(isAdmin);
        }
        if (adminButton != null) {
            adminButton.setVisible(isAdmin);
            adminButton.setManaged(isAdmin);
        }
        chargerEvenements();
    }

    private void chargerEvenements() {
        try {
            typesEvenement = typeService.getAll();
            typeService.garantirTypesParDefaut();
            List<Evenement> liste = evenementService.getAll();
            afficherEvenements(liste);
        } catch (Exception e) {
            afficherErreur(e.getMessage());
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
    private void rechercher() {
        String titre = tfRechercheTitre.getText().trim();
        String lieu = tfRechercheLieu.getText().trim();
        try {
            List<Evenement> liste;
            if (!lieu.isEmpty()) {
                liste = evenementService.rechercherParLieu(lieu);
            } else if (!titre.isEmpty()) {
                liste = evenementService.rechercherParTitre(titre);
            } else {
                liste = evenementService.getAll();
            }
            afficherEvenements(liste);
        } catch (Exception e) {
            afficherErreur(e.getMessage());
        }
    }

    @FXML
    private void reinitialiser() {
        tfRechercheTitre.clear();
        tfRechercheLieu.clear();
        chargerEvenements();
    }

    private VBox creerCarteEvenement(Evenement evenement) {
        User currentUser = SessionManager.getCurrentUser();
        boolean isAdmin = currentUser != null && currentUser.getRole() == UserRole.ADMIN;

        ImageView imageView = new ImageView();
        imageView.setFitWidth(260);
        imageView.setFitHeight(146);
        imageView.setPreserveRatio(false);

        StackPane imageBox = new StackPane();
        imageBox.setPrefSize(260, 146);
        imageBox.setMinSize(260, 146);
        imageBox.setMaxSize(260, 146);
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

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button btnDetails = new Button("Details");
        btnDetails.getStyleClass().addAll("success-button", "compact-action-button");
        btnDetails.setOnAction(e -> ouvrirDetails(evenement));

        Button btnModifier = new Button("Modifier");
        btnModifier.getStyleClass().addAll("warning-button", "compact-action-button");
        btnModifier.setVisible(isAdmin);
        btnModifier.setManaged(isAdmin);
        btnModifier.setOnAction(e -> ouvrirModifier(evenement));

        Button btnSupprimer = new Button("Suppr.");
        btnSupprimer.getStyleClass().addAll("danger-button", "compact-action-button");
        btnSupprimer.setVisible(isAdmin);
        btnSupprimer.setManaged(isAdmin);
        btnSupprimer.setOnAction(e -> supprimerEvenement(evenement));

        actions.getChildren().addAll(btnDetails, btnModifier, btnSupprimer);

        VBox details = new VBox(8, titre, badges, lieu, date, capacite, actions);
        details.getStyleClass().add("event-card-body");

        VBox card = new VBox(imageBox, details);
        card.getStyleClass().add("event-card");
        card.setPrefWidth(260);
        card.setMinWidth(260);
        card.setMaxWidth(260);
        return card;
    }

    private Image chargerImage(String chemin) {
        if (chemin == null || chemin.isBlank()) return null;
        try {
            if (chemin.startsWith("/")) {
                Image img = new Image(getClass().getResourceAsStream(chemin), 260, 146, true, true);
                return img.isError() ? null : img;
            }
            String source = chemin.startsWith("http://") || chemin.startsWith("https://") || chemin.startsWith("file:")
                    ? chemin
                    : new File(chemin).toURI().toString();
            Image image = new Image(source, 260, 146, false, true, false);
            return image.isError() ? null : image;
        } catch (Exception e) {
            return null;
        }
    }

    private String libelleType(int typeId) {
        if (typesEvenement == null) return "Inconnu";
        return typesEvenement.stream()
                .filter(t -> t.getId() == typeId)
                .map(TypeEvenement::getLibelle)
                .findFirst()
                .orElse("Inconnu");
    }

    private String valeurOuTiret(String valeur) {
        return valeur == null || valeur.isBlank() ? "-" : valeur;
    }

    @FXML
    private void ouvrirAjouter() {
        try {
            EvenementFormController.evenementToEdit = null;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/EvenementForm.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) eventsFlow.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Team Hub - Nouvel Evenement");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            afficherErreur(e.getMessage());
        }
    }

    private void ouvrirModifier(Evenement evenement) {
        if (evenement == null) return;
        try {
            EvenementFormController.evenementToEdit = evenement;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/EvenementForm.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) eventsFlow.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Team Hub - Modifier Evenement");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            afficherErreur(e.getMessage());
        }
    }

    private void supprimerEvenement(Evenement evenement) {
        if (evenement == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Voulez-vous vraiment supprimer l'evenement \"" + evenement.getTitre() + "\" ?");
        Optional<ButtonType> response = confirm.showAndWait();
        if (response.isPresent() && response.get() == ButtonType.OK) {
            try {
                evenementService.supprimer(evenement.getId());
                chargerEvenements();
                afficherInformation("Evenement supprime avec succes");
            } catch (Exception e) {
                afficherErreur(e.getMessage());
            }
        }
    }

    private void ouvrirDetails(Evenement evenement) {
        if (evenement == null) return;
        try {
            DetailsEvenementController.evenementAffiche = evenement;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DetailsEvenement.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) eventsFlow.getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 800));
            stage.setTitle("Team Hub - Details de l'evenement");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            afficherErreur("Erreur lors de l'ouverture des details");
        }
    }

    @FXML
    private void ouvrirAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AdminInscription.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) eventsFlow.getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Administration des inscriptions");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            afficherErreur("Erreur ouverture admin inscriptions");
        }
    }

    @FXML
    private void ouvrirInscriptionUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/GestionInscription.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) eventsFlow.getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Inscription aux evenements");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            afficherErreur(e.getMessage());
        }
    }

    @FXML
    private void handleHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainInterface.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) eventsFlow.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setTitle("Team Hub - E-Sport Recruitment");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMyPosts() {
        handleHome();
    }

    @FXML
    private void handleAnnonces() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Annonces.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) eventsFlow.getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Gestion des Annonces");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Profile.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) eventsFlow.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Team Hub - Profile");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AdminDashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) eventsFlow.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setTitle("Admin Dashboard - Team Hub");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
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
