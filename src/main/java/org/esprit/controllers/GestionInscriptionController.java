package org.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.esprit.models.Evenement;
import org.esprit.models.Inscription;
import org.esprit.services.InscriptionService;
import org.esprit.utils.UiEffects;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class GestionInscriptionController {

    @FXML private Parent rootPane;
    @FXML private Label lbEvenement;
    @FXML private TextField tfUtilisateurId;
    @FXML private TableView<Inscription> tableInscriptions;
    @FXML private TableColumn<Inscription, Integer> colId;
    @FXML private TableColumn<Inscription, Integer> colUtilisateurId;
    @FXML private TableColumn<Inscription, LocalDateTime> colDateInscription;
    @FXML private TableColumn<Inscription, String> colStatut;

    private InscriptionService service = new InscriptionService();
    private Evenement evenement;

    @FXML
    void initialize() {
        UiEffects.applyEntranceAndHover(rootPane);

        evenement = GestionEvenementController.evenementSelectionne;

        if (evenement != null) {
            lbEvenement.setText("Événement : " + evenement.getTitre());
        }


        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUtilisateurId.setCellValueFactory(new PropertyValueFactory<>("utilisateurId"));
        colDateInscription.setCellValueFactory(new PropertyValueFactory<>("dateInscription"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        chargerInscriptions();
    }

    private void chargerInscriptions() {
        List<Inscription> liste = service.getAll();
        ObservableList<Inscription> data = FXCollections.observableArrayList(liste);
        tableInscriptions.setItems(data);
    }

    @FXML
    void inscrire(ActionEvent event) {
        try {
            int utilisateurId = Integer.parseInt(tfUtilisateurId.getText());
            String statut = "En attente";

            if (statut == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setContentText("Veuillez choisir un statut !");
                alert.show();
                return;
            }

            Inscription insc = new Inscription(evenement.getId(), utilisateurId, statut);
            service.add(insc);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Inscription ajoutée !");
            alert.show();

            tfUtilisateurId.clear();
            chargerInscriptions();

        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("L'ID utilisateur doit être un nombre !");
            alert.show();
        }
    }

    @FXML
    void supprimer(ActionEvent event) {
        Inscription selected = tableInscriptions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setContentText("Veuillez sélectionner une inscription !");
            alert.show();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Voulez-vous supprimer cette inscription ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                service.delete(selected);
                chargerInscriptions();
            }
        });
    }

    @FXML
    void retour(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GestionEvenement.fxml"));
            tableInscriptions.getScene().setRoot(root);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    @FXML
    void confirmerInscription(ActionEvent event) {
        Inscription selected = tableInscriptions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Sélectionnez une inscription !").show();
            return;
        }
        selected.setStatut("Confirmé");
        service.update(selected);
        chargerInscriptions();
    }

    @FXML
    void annulerInscription(ActionEvent event) {
        Inscription selected = tableInscriptions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Sélectionnez une inscription !").show();
            return;
        }
        selected.setStatut("Annulé");
        service.update(selected);
        chargerInscriptions();
    }
}
