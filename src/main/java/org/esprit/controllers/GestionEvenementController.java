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
import org.esprit.services.EvenementService;
import org.esprit.models.TypeEvenement;
import org.esprit.services.TypeEvenementService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class GestionEvenementController {

    @FXML private TableView<Evenement> tableEvenements;
    @FXML private TableColumn<Evenement, Integer> colId;
    @FXML private TableColumn<Evenement, String> colTitre;
    @FXML private TableColumn<Evenement, String> colType;
    @FXML private TableColumn<Evenement, String> colLieu;
    @FXML private TableColumn<Evenement, LocalDateTime> colDateDebut;
    @FXML private TableColumn<Evenement, LocalDateTime> colDateFin;
    @FXML private TableColumn<Evenement, String> colStatut;
    @FXML private TextField tfRecherchetitre;
    @FXML private TextField tfRechercheLieu;

    private TypeEvenementService typeService = new TypeEvenementService();
    private EvenementService service = new EvenementService();
    public static Evenement evenementSelectionne = null;

    @FXML
    void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colType.setCellValueFactory(cellData -> {
            int typeId = cellData.getValue().getTypeId();
            List<TypeEvenement> types = typeService.getAll();
            String libelle = types.stream()
                    .filter(t -> t.getId() == typeId)
                    .map(TypeEvenement::getLibelle)
                    .findFirst()
                    .orElse("Inconnu");
            return new javafx.beans.property.SimpleStringProperty(libelle);
        });
        colLieu.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));


        chargerEvenements();
    }

    private void chargerEvenements() {
        List<Evenement> liste = service.getAll();
        ObservableList<Evenement> data = FXCollections.observableArrayList(liste);
        tableEvenements.setItems(data);
    }

    @FXML
    void rechercher(ActionEvent event) {
        String titre = tfRecherchetitre.getText();
        String lieu = tfRechercheLieu.getText();
        List<Evenement> liste;
        if (!lieu.isEmpty()) {
            liste = service.rechercherParLieu(lieu);
        } else {
            liste = service.rechercherParTitre(titre);
        }
        tableEvenements.setItems(FXCollections.observableArrayList(liste));
    }

    @FXML
    void reinitialiser(ActionEvent event) {
        tfRecherchetitre.clear();
        tfRechercheLieu.clear();
        chargerEvenements();
    }

    @FXML
    void ouvrirAjouter(ActionEvent event) {
        try {
            AjouterEvenementController.evenementAModifier = null;
            Parent root = FXMLLoader.load(getClass().getResource("/AjouterEvenement.fxml"));
            tableEvenements.getScene().setRoot(root);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    void ouvrirModifier(ActionEvent event) {
        evenementSelectionne = tableEvenements.getSelectionModel().getSelectedItem();
        if (evenementSelectionne == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setContentText("Veuillez sélectionner un événement !");
            alert.show();
            return;
        }
        try {
            AjouterEvenementController.evenementAModifier = evenementSelectionne;
            Parent root = FXMLLoader.load(getClass().getResource("/AjouterEvenement.fxml"));
            tableEvenements.getScene().setRoot(root);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    void supprimer(ActionEvent event) {
        Evenement selected = tableEvenements.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setContentText("Veuillez sélectionner un événement !");
            alert.show();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Voulez-vous supprimer cet événement ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                service.delete(selected);
                chargerEvenements();
            }
        });
    }

    @FXML
    void ouvrirDetails(ActionEvent event) {
        evenementSelectionne = tableEvenements.getSelectionModel().getSelectedItem();
        if (evenementSelectionne == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setContentText("Veuillez sélectionner un événement !");
            alert.show();
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/DetailsEvenement.fxml"));
            tableEvenements.getScene().setRoot(root);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    void ouvrirInscriptions(ActionEvent event) {
        evenementSelectionne = tableEvenements.getSelectionModel().getSelectedItem();
        if (evenementSelectionne == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setContentText("Veuillez sélectionner un événement !");
            alert.show();
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GestionInscription.fxml"));
            tableEvenements.getScene().setRoot(root);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

}