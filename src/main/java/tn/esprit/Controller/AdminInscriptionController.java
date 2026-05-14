package tn.esprit.Controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.entities.Inscription;
import tn.esprit.services.InscriptionService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class AdminInscriptionController {

    @FXML private TableView<Inscription> tableInscriptions;
    @FXML private TableColumn<Inscription, Integer> colId;
    @FXML private TableColumn<Inscription, String> colEvenement;
    @FXML private TableColumn<Inscription, String> colUtilisateur;
    @FXML private TableColumn<Inscription, Date> colDateInscription;
    @FXML private TableColumn<Inscription, String> colStatut;
    @FXML private TableColumn<Inscription, Void> colActions;
    @FXML private Label lbTotal;
    @FXML private Label lbEnAttente;
    @FXML private Label lbConfirmes;

    private final InscriptionService service = new InscriptionService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        colId.setVisible(false);
        colEvenement.setCellValueFactory(new PropertyValueFactory<>("nomEvenement"));
        colUtilisateur.setCellValueFactory(new PropertyValueFactory<>("nomUser"));
        colDateInscription.setCellValueFactory(new PropertyValueFactory<>("dateInscription"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        colDateInscription.setCellFactory(column -> new TableCell<>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(fmt.format(item.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
                }
            }
        });

        colStatut.setCellFactory(column -> new TableCell<>() {
            private final Label label = new Label();
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setGraphic(null);
                    return;
                }
                label.setText(statut);
                String style = "-fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 11px;";
                if (statut.contains("Confirm")) {
                    label.setStyle(style + " -fx-background-color: #1a3a2a; -fx-text-fill: #00ff88;");
                } else if (statut.contains("Annul")) {
                    label.setStyle(style + " -fx-background-color: #3a1a1a; -fx-text-fill: #ff4b4b;");
                } else {
                    label.setStyle(style + " -fx-background-color: #1e3a5f; -fx-text-fill: #4fc3f7;");
                }
                setGraphic(label);
            }
        });

        configurerActions();
        tableInscriptions.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        chargerInscriptions();
    }

    private void configurerActions() {
        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnConfirmer = new Button("Confirmer");
            private final Button btnAnnuler = new Button("Annuler");
            private final Button btnSupprimer = new Button("Suppr.");
            private final HBox actions = new HBox(8, btnConfirmer, btnAnnuler, btnSupprimer);

            {
                actions.setAlignment(Pos.CENTER);
                btnConfirmer.setStyle("-fx-background-color: #1a3a2a; -fx-text-fill: #00ff88; -fx-border-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;");
                btnAnnuler.setStyle("-fx-background-color: #3a2a1a; -fx-text-fill: #ffa500; -fx-border-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;");
                btnSupprimer.setStyle("-fx-background-color: #3a1a1a; -fx-text-fill: #ff4b4b; -fx-border-radius: 6; -fx-padding: 6 12; -fx-cursor: hand;");

                btnConfirmer.setOnAction(event -> confirmer(getTableView().getItems().get(getIndex())));
                btnAnnuler.setOnAction(event -> annuler(getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(event -> supprimer(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });
    }

    private void chargerInscriptions() {
        try {
            List<Inscription> liste = service.getAll();
            ObservableList<Inscription> data = FXCollections.observableArrayList(liste);
            tableInscriptions.setItems(data);

            long enAttente = liste.stream()
                    .filter(i -> "En attente".equalsIgnoreCase(i.getStatut()))
                    .count();
            long confirmes = liste.stream()
                    .filter(i -> i.getStatut() != null && i.getStatut().toLowerCase().contains("confirm"))
                    .count();

            lbTotal.setText(String.valueOf(liste.size()));
            lbEnAttente.setText(String.valueOf(enAttente));
            lbConfirmes.setText(String.valueOf(confirmes));
        } catch (Exception e) {
            afficherErreur("Erreur chargement inscriptions: " + e.getMessage());
        }
    }

    private void confirmer(Inscription inscription) {
        try {
            service.updateStatut(inscription.getId(), "Confirmé");
            chargerInscriptions();
            afficherInfo("Inscription confirmee");
        } catch (Exception e) {
            afficherErreur(e.getMessage());
        }
    }

    private void annuler(Inscription inscription) {
        try {
            service.updateStatut(inscription.getId(), "Annulé");
            chargerInscriptions();
            afficherInfo("Inscription annulee");
        } catch (Exception e) {
            afficherErreur(e.getMessage());
        }
    }

    private void supprimer(Inscription inscription) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setContentText("Supprimer cette inscription ?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                service.supprimer(inscription.getId());
                chargerInscriptions();
                afficherInfo("Inscription supprimee");
            } catch (Exception e) {
                afficherErreur(e.getMessage());
            }
        }
    }

    @FXML
    private void retourEvenements(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Evenements.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Gestion des Evenements");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            afficherErreur(e.getMessage());
        }
    }

    private void afficherInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
