package tn.esprit.Controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.entities.Evenement;
import tn.esprit.entities.TypeEvenement;
import tn.esprit.entities.User;
import tn.esprit.entities.UserRole;
import tn.esprit.services.EvenementService;
import tn.esprit.services.TypeEvenementService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class EvenementAdminController {

    @FXML private TableView<Evenement> tableEvenements;
    @FXML private TableColumn<Evenement, String> colTitre;
    @FXML private TableColumn<Evenement, String> colType;
    @FXML private TableColumn<Evenement, String> colLieu;
    @FXML private TableColumn<Evenement, Date> colDateDebut;
    @FXML private TableColumn<Evenement, Date> colDateFin;
    @FXML private TableColumn<Evenement, Void> colActions;
    @FXML private TextField tfRechercheTitre;
    @FXML private TextField tfRechercheLieu;
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
        if (btnAdminInscriptionsFooter != null) {
            btnAdminInscriptionsFooter.setVisible(isAdmin);
            btnAdminInscriptionsFooter.setManaged(isAdmin);
        }
        if (adminButton != null) {
            adminButton.setVisible(isAdmin);
            adminButton.setManaged(isAdmin);
        }

        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colType.setCellValueFactory(cellData -> {
            int typeId = cellData.getValue().getTypeId();
            String libelle = "Inconnu";
            if (typesEvenement != null) {
                libelle = typesEvenement.stream()
                        .filter(t -> t.getId() == typeId)
                        .map(TypeEvenement::getLibelle)
                        .findFirst()
                        .orElse("Inconnu");
            }
            return new SimpleStringProperty(libelle);
        });
        colLieu.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));

        colDateDebut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : dateFormat.format(item));
            }
        });
        colDateFin.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : dateFormat.format(item));
            }
        });

        tableEvenements.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        configurerActions();
        chargerEvenements();
    }

    private void configurerActions() {
        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnDetails = new Button("Details");
            private final Button btnModifier = new Button("Modifier");
            private final Button btnSupprimer = new Button("Suppr.");
            private final HBox actions = new HBox(8, btnDetails, btnModifier, btnSupprimer);

            {
                actions.setAlignment(Pos.CENTER);
                btnDetails.getStyleClass().addAll("success-button", "compact-action-button");
                btnModifier.getStyleClass().addAll("warning-button", "compact-action-button");
                btnSupprimer.getStyleClass().addAll("danger-button", "compact-action-button");

                btnDetails.setOnAction(event -> ouvrirDetails(getTableView().getItems().get(getIndex())));
                btnModifier.setOnAction(event -> ouvrirModifier(getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(event -> supprimerEvenement(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });
    }

    private void chargerEvenements() {
        try {
            typesEvenement = typeService.getAll();
            typeService.garantirTypesParDefaut();
            List<Evenement> liste = evenementService.getAll();
            tableEvenements.setItems(FXCollections.observableArrayList(liste));
        } catch (Exception e) {
            afficherErreur(e.getMessage());
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
            tableEvenements.setItems(FXCollections.observableArrayList(liste));
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

    @FXML
    private void ouvrirAjouter() {
        try {
            EvenementFormController.evenementToEdit = null;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/EvenementForm.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tableEvenements.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Team Hub - Nouvel Evenement");
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
            Stage stage = (Stage) tableEvenements.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Team Hub - Modifier Evenement");
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
            Stage stage = (Stage) tableEvenements.getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 800));
            stage.setTitle("Team Hub - Details de l'evenement");
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
            Stage stage = (Stage) tableEvenements.getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Administration des inscriptions");
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
            Stage stage = (Stage) tableEvenements.getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Inscription aux evenements");
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
            Stage stage = (Stage) tableEvenements.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setTitle("Team Hub - E-Sport Recruitment");
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
            Stage stage = (Stage) tableEvenements.getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Gestion des Annonces");
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
            Stage stage = (Stage) tableEvenements.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
            stage.setTitle("Team Hub - Profile");
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
            Stage stage = (Stage) tableEvenements.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.setTitle("Admin Dashboard - Team Hub");
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
