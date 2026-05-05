package org.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import org.esprit.models.Annonce;
import org.esprit.models.Categorie;
import org.esprit.services.AnnonceService;
import org.esprit.services.CategorieService;

import java.util.Date;
import java.util.List;

public class AjouterAnnonceController {

    public static Annonce annonceToEdit;

    @FXML
    private TextField tfTitre;

    @FXML
    private TextField tfDescription;

    @FXML
    private TextField tfJeu;

    @FXML
    private TextField tfSalaire;

    @FXML
    private ComboBox<String> cbStatut;

    @FXML
    private ComboBox<Categorie> cbCategorie;

    private final AnnonceService annonceService = new AnnonceService();
    private final CategorieService categorieService = new CategorieService();

    @FXML
    private void initialize() {
        cbStatut.setItems(FXCollections.observableArrayList("OUVERTE", "FERMEE", "EN_ATTENTE"));
        cbCategorie.setConverter(new StringConverter<>() {
            @Override
            public String toString(Categorie categorie) {
                return categorie == null ? "" : categorie.getNom();
            }

            @Override
            public Categorie fromString(String string) {
                return null;
            }
        });

        chargerCategories();

        if (annonceToEdit != null) {
            remplirFormulaire();
        } else {
            cbStatut.setValue("OUVERTE");
        }
    }

    @FXML
    private void enregistrerAnnonce() {
        try {
            Categorie categorie = cbCategorie.getValue();
            if (categorie == null) {
                throw new Exception("Veuillez choisir une categorie.");
            }

            Annonce annonce = annonceToEdit == null ? new Annonce() : annonceToEdit;
            annonce.setTitre(tfTitre.getText());
            annonce.setDescription(tfDescription.getText());
            annonce.setJeu(tfJeu.getText());
            annonce.setSalaire(parseSalaire());
            annonce.setStatut(cbStatut.getValue());
            annonce.setIdCategorie(categorie.getId());
            annonce.setNomCategorie(categorie.getNom());

            if (annonceToEdit == null) {
                annonce.setDatePublication(new Date());
                annonceService.ajouter(annonce);
            } else {
                annonceService.modifier(annonce);
            }

            annonceToEdit = null;
            retourGestion();
        } catch (Exception e) {
            afficherErreur(e.getMessage());
        }
    }

    @FXML
    private void annuler() {
        annonceToEdit = null;
        retourGestion();
    }

    private void chargerCategories() {
        try {
            List<Categorie> categories = categorieService.getAll();
            cbCategorie.setItems(FXCollections.observableArrayList(categories));
        } catch (Exception e) {
            afficherErreur(e.getMessage());
        }
    }

    private void remplirFormulaire() {
        tfTitre.setText(annonceToEdit.getTitre());
        tfDescription.setText(annonceToEdit.getDescription());
        tfJeu.setText(annonceToEdit.getJeu());
        tfSalaire.setText(String.valueOf(annonceToEdit.getSalaire()));
        cbStatut.setValue(annonceToEdit.getStatut());

        for (Categorie categorie : cbCategorie.getItems()) {
            if (categorie.getId() == annonceToEdit.getIdCategorie()) {
                cbCategorie.setValue(categorie);
                break;
            }
        }
    }

    private double parseSalaire() throws Exception {
        try {
            return Double.parseDouble(tfSalaire.getText().trim());
        } catch (NumberFormatException e) {
            throw new Exception("Le salaire doit etre un nombre valide.");
        }
    }

    private void retourGestion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/GestionAnnonce.fxml"));
            Parent root = loader.load();
            tfTitre.getScene().setRoot(root);
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
