package org.esprit.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.esprit.models.Evenement;
import org.esprit.models.Inscription;
import org.esprit.services.EvenementService;
import org.esprit.services.InscriptionService;
import org.esprit.security.AccessControl;
import org.esprit.utils.UiEffects;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminInscriptionController {

    @FXML private Parent rootPane;
    @FXML private TableView<Inscription> tableInscriptions;
    @FXML private TableColumn<Inscription, Integer> colId;
    @FXML private TableColumn<Inscription, Integer> colEvenementId;
    @FXML private TableColumn<Inscription, LocalDateTime> colDateInscription;
    @FXML private TableColumn<Inscription, String> colStatut;
    @FXML private TableColumn<Inscription, String> colEvenement;
    @FXML private TableColumn<Inscription, Void> colActions;
    @FXML private Label lbTotal;
    @FXML private Label lbEnAttente;
    @FXML private Label lbConfirmes;

    private final InscriptionService service = new InscriptionService();
    private final EvenementService evenementService = new EvenementService();
    private Map<Integer, String> titresEvenements;

    @FXML
    void initialize() {
        UiEffects.applyEntranceAndHover(rootPane);

        if (!AccessControl.requireAdmin()) {
            Platform.runLater(() -> ouvrirPageSilencieusement("/GestionInscription.fxml"));
            return;
        }

        colId.setVisible(false);
        colEvenementId.setVisible(false);
        colDateInscription.setCellValueFactory(new PropertyValueFactory<>("dateInscription"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colEvenement.setCellValueFactory(cellData -> {
            int evenementId = cellData.getValue().getEvenementId();
            String titre = titresEvenements.getOrDefault(evenementId, "Evenement #" + evenementId);
            return new javafx.beans.property.SimpleStringProperty(titre);
        });
        configurerStatut();
        configurerActions();
        tableInscriptions.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        chargerInscriptions();
    }

    private void chargerInscriptions() {
        titresEvenements = evenementService.getAll().stream()
                .collect(Collectors.toMap(Evenement::getId, Evenement::getTitre));
        List<Inscription> liste = service.getAll();
        ObservableList<Inscription> data = FXCollections.observableArrayList(liste);
        tableInscriptions.setItems(data);
        mettreAJourStatistiques(liste);
    }

    private void mettreAJourStatistiques(List<Inscription> inscriptions) {
        long enAttente = inscriptions.stream()
                .filter(inscription -> "En attente".equalsIgnoreCase(inscription.getStatut()))
                .count();
        long confirmes = inscriptions.stream()
                .filter(inscription -> "Confirmé".equalsIgnoreCase(inscription.getStatut()) ||
                        "Confirme".equalsIgnoreCase(inscription.getStatut()))
                .count();

        lbTotal.setText(String.valueOf(inscriptions.size()));
        lbEnAttente.setText(String.valueOf(enAttente));
        lbConfirmes.setText(String.valueOf(confirmes));
    }

    private void configurerStatut() {
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
                label.getStyleClass().setAll("status-pill", styleStatut(statut));
                setGraphic(label);
            }
        });
    }

    private String styleStatut(String statut) {
        String normalized = statut.toLowerCase();
        if (normalized.contains("confirm")) {
            return "status-confirmed";
        }
        if (normalized.contains("annul")) {
            return "status-cancelled";
        }
        return "status-pending";
    }

    private void configurerActions() {
        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnConfirmer = creerBoutonAction("Confirmer", "Confirmer cette inscription", 86);
            private final Button btnAnnuler = creerBoutonAction("Annuler", "Annuler cette inscription", 76);
            private final Button btnSupprimer = creerBoutonAction("Suppr.", "Supprimer cette inscription", 72);
            private final HBox actions = new HBox(8, btnConfirmer, btnAnnuler, btnSupprimer);

            {
                actions.setAlignment(Pos.CENTER);
                btnConfirmer.getStyleClass().add("success-button");
                btnAnnuler.getStyleClass().add("warning-button");
                btnSupprimer.getStyleClass().add("danger-button");

                btnConfirmer.setOnAction(event -> confirmerInscription(getTableView().getItems().get(getIndex())));
                btnAnnuler.setOnAction(event -> annulerInscription(getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(event -> supprimer(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actions);
            }
        });
    }

    private Button creerBoutonAction(String texte, String tooltip, double largeur) {
        Button button = new Button(texte);
        button.setTooltip(new Tooltip(tooltip));
        button.setMinWidth(largeur);
        button.setPrefWidth(largeur);
        button.setMaxWidth(largeur);
        button.setMinHeight(30);
        button.setPrefHeight(30);
        button.setMaxHeight(30);
        button.getStyleClass().add("compact-action-button");
        return button;
    }

    @FXML
    void confirmerInscription(ActionEvent event) {
        if (!AccessControl.requireAdmin()) {
            return;
        }
        Inscription selected = getInscriptionSelectionnee();
        if (selected == null) {
            return;
        }
        selected.setStatut("Confirm\u00e9");
        service.update(selected);
        chargerInscriptions();
    }

    private void confirmerInscription(Inscription inscription) {
        if (!AccessControl.requireAdmin()) {
            return;
        }
        if (inscription == null) {
            return;
        }
        inscription.setStatut("Confirmé");
        service.update(inscription);
        chargerInscriptions();
    }

    @FXML
    void annulerInscription(ActionEvent event) {
        if (!AccessControl.requireAdmin()) {
            return;
        }
        Inscription selected = getInscriptionSelectionnee();
        if (selected == null) {
            return;
        }
        selected.setStatut("Annul\u00e9");
        service.update(selected);
        chargerInscriptions();
    }

    private void annulerInscription(Inscription inscription) {
        if (!AccessControl.requireAdmin()) {
            return;
        }
        if (inscription == null) {
            return;
        }
        inscription.setStatut("Annulé");
        service.update(inscription);
        chargerInscriptions();
    }

    @FXML
    void supprimer(ActionEvent event) {
        if (!AccessControl.requireAdmin()) {
            return;
        }
        Inscription selected = getInscriptionSelectionnee();
        if (selected == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Voulez-vous supprimer cette inscription ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                service.delete(selected);
                chargerInscriptions();
            }
        });
    }

    private void supprimer(Inscription inscription) {
        if (!AccessControl.requireAdmin()) {
            return;
        }
        if (inscription == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Voulez-vous supprimer cette inscription ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                service.delete(inscription);
                chargerInscriptions();
            }
        });
    }

    @FXML
    void retour(ActionEvent event) {
        if (!AccessControl.requireAdmin()) {
            ouvrirPageSilencieusement("/GestionInscription.fxml");
            return;
        }
        ouvrirPageSilencieusement("/GestionEvenement.fxml");
    }

    private void ouvrirPageSilencieusement(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            tableInscriptions.getScene().setRoot(root);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private Inscription getInscriptionSelectionnee() {
        Inscription selected = tableInscriptions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("Veuillez selectionner une inscription !");
            alert.show();
        }
        return selected;
    }
}
