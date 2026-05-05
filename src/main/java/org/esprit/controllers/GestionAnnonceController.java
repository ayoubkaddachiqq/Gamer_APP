package org.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import org.esprit.models.Annonce;
import org.esprit.services.AnnonceService;

import java.util.Optional;

public class GestionAnnonceController {

    @FXML
    private TableView<Annonce> tableAnnonces;

    @FXML
    private TableColumn<Annonce, Integer> colId;

    @FXML
    private TableColumn<Annonce, String> colTitre;

    @FXML
    private TableColumn<Annonce, String> colJeu;

    @FXML
    private TableColumn<Annonce, Double> colSalaire;

    @FXML
    private TableColumn<Annonce, String> colStatut;

    @FXML
    private TableColumn<Annonce, String> colCategorie;

    @FXML
    private TextField tfRecherche;

    private final AnnonceService annonceService = new AnnonceService();

    @FXML
    private void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colJeu.setCellValueFactory(new PropertyValueFactory<>("jeu"));
        colSalaire.setCellValueFactory(new PropertyValueFactory<>("salaire"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colCategorie.setCellValueFactory(new PropertyValueFactory<>("nomCategorie"));
        chargerAnnonces();
    }

    @FXML
    private void ajouterAnnonce() {
        AjouterAnnonceController.annonceToEdit = null;
        ouvrirFormulaire();
    }

    @FXML
    private void modifierAnnonce() {
        Annonce annonce = tableAnnonces.getSelectionModel().getSelectedItem();
        if (annonce == null) {
            afficherErreur("Veuillez selectionner une annonce a modifier.");
            return;
        }

        AjouterAnnonceController.annonceToEdit = annonce;
        ouvrirFormulaire();
    }

    @FXML
    private void supprimerAnnonce() {
        Annonce annonce = tableAnnonces.getSelectionModel().getSelectedItem();
        if (annonce == null) {
            afficherErreur("Veuillez selectionner une annonce a supprimer.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer l'annonce");
        alert.setContentText("Voulez-vous vraiment supprimer cette annonce ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                annonceService.supprimer(annonce.getId());
                chargerAnnonces();
            } catch (Exception e) {
                afficherErreur(e.getMessage());
            }
        }
    }

    @FXML
    private void rechercherAnnonce() {
        try {
            String keyword = tfRecherche.getText();
            if (keyword == null || keyword.trim().isEmpty()) {
                chargerAnnonces();
            } else {
                tableAnnonces.setItems(FXCollections.observableArrayList(annonceService.rechercher(keyword.trim())));
            }
        } catch (Exception e) {
            afficherErreur(e.getMessage());
        }
    }

    private void chargerAnnonces() {
        try {
            tableAnnonces.setItems(FXCollections.observableArrayList(annonceService.getAll()));
        } catch (Exception e) {
            afficherErreur(e.getMessage());
        }
    }

    private void ouvrirFormulaire() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterAnnonce.fxml"));
            Parent root = loader.load();
            tableAnnonces.getScene().setRoot(root);
        } catch (Exception e) {
            afficherErreur(e.getMessage());
        }
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
