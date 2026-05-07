package org.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.esprit.models.Inscription;
import org.esprit.services.InscriptionService;
import org.esprit.utils.UiEffects;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class AdminInscriptionController {

    @FXML private Parent rootPane;
    @FXML private TableView<Inscription> tableInscriptions;
    @FXML private TableColumn<Inscription, Integer> colId;
    @FXML private TableColumn<Inscription, Integer> colEvenementId;
    @FXML private TableColumn<Inscription, Integer> colUtilisateurId;
    @FXML private TableColumn<Inscription, LocalDateTime> colDateInscription;
    @FXML private TableColumn<Inscription, String> colStatut;

    private final InscriptionService service = new InscriptionService();

    @FXML
    void initialize() {
        UiEffects.applyEntranceAndHover(rootPane);

        colId.setVisible(false);
        colEvenementId.setVisible(false);
        colUtilisateurId.setVisible(false);
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
    void confirmerInscription(ActionEvent event) {
        Inscription selected = getInscriptionSelectionnee();
        if (selected == null) {
            return;
        }
        selected.setStatut("Confirm\u00e9");
        service.update(selected);
        chargerInscriptions();
    }

    @FXML
    void annulerInscription(ActionEvent event) {
        Inscription selected = getInscriptionSelectionnee();
        if (selected == null) {
            return;
        }
        selected.setStatut("Annul\u00e9");
        service.update(selected);
        chargerInscriptions();
    }

    @FXML
    void supprimer(ActionEvent event) {
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

    @FXML
    void retour(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GestionEvenement.fxml"));
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
