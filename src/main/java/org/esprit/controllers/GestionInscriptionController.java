package org.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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

    private final InscriptionService service = new InscriptionService();
    private Evenement evenement;

    @FXML
    void initialize() {
        UiEffects.applyEntranceAndHover(rootPane);

        evenement = GestionEvenementController.evenementSelectionne;

        if (evenement != null) {
            lbEvenement.setText("Evenement : " + evenement.getTitre());
        }

        colId.setVisible(false);
        colUtilisateurId.setVisible(false);
        colDateInscription.setCellValueFactory(new PropertyValueFactory<>("dateInscription"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        chargerInscriptions();
    }

    private void chargerInscriptions() {
        List<Inscription> liste = service.getAll();
        if (evenement != null) {
            liste = liste.stream()
                    .filter(inscription -> inscription.getEvenementId() == evenement.getId())
                    .toList();
        }
        ObservableList<Inscription> data = FXCollections.observableArrayList(liste);
        tableInscriptions.setItems(data);
    }

    @FXML
    void inscrire(ActionEvent event) {
        try {
            if (evenement == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setContentText("Veuillez selectionner un evenement !");
                alert.show();
                return;
            }

            int utilisateurId = Integer.parseInt(tfUtilisateurId.getText());
            Inscription insc = new Inscription(evenement.getId(), utilisateurId, "En attente");
            service.add(insc);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Inscription ajoutee !");
            alert.show();

            tfUtilisateurId.clear();
            chargerInscriptions();
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("L'ID utilisateur doit etre un nombre !");
            alert.show();
        }
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
}
