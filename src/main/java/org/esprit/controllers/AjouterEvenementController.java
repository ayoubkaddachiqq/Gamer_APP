package org.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.esprit.models.Evenement;
import org.esprit.models.TypeEvenement;
import org.esprit.services.EvenementService;
import org.esprit.services.TypeEvenementService;
import org.esprit.utils.UiEffects;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class AjouterEvenementController {

    @FXML private Parent rootPane;
    @FXML private TextField tfTitre;
    @FXML private TextArea taDescription;
    @FXML private ComboBox<TypeEvenement> cbType;
    @FXML private TextField tfDateDebut;
    @FXML private TextField tfDateFin;
    @FXML private TextField tfLieu;
    @FXML private TextField tfNbMax;
    @FXML private ComboBox<String> cbStatut;
    @FXML private TextField tfImage;
    @FXML private WebView mapView;

    public static Evenement evenementAModifier = null;

    private EvenementService service = new EvenementService();
    private TypeEvenementService typeService = new TypeEvenementService();

    @FXML
    void initialize() {
        UiEffects.applyEntranceAndHover(rootPane);

        // Charger les types
        List<TypeEvenement> types = typeService.getAll();
        cbType.getItems().addAll(types);

        // Charger les statuts
        cbStatut.getItems().addAll("Planifié", "En cours", "Terminé", "Annulé");

        if (evenementAModifier != null) {
            tfTitre.setText(evenementAModifier.getTitre());
            taDescription.setText(evenementAModifier.getDescription());
            tfDateDebut.setText(evenementAModifier.getDateDebut().toString());
            tfDateFin.setText(evenementAModifier.getDateFin().toString());
            tfLieu.setText(evenementAModifier.getLieu());
            tfNbMax.setText(String.valueOf(evenementAModifier.getNbParticipantsMax()));
            cbStatut.setValue(evenementAModifier.getStatut());
            if (evenementAModifier.getImage() != null) {
                tfImage.setText(evenementAModifier.getImage());
            }
        }
    }

    @FXML
    void choisirImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File file = fileChooser.showOpenDialog(tfImage.getScene().getWindow());
        if (file != null) {
            tfImage.setText(file.getAbsolutePath());
        }
    }

    @FXML
    void ouvrirMaps(ActionEvent event) {
        String lieu = tfLieu.getText();
        if (!lieu.isEmpty()) {
            String url = "https://maps.google.com/?q=" + lieu.replace(" ", "+");
            mapView.getEngine().load(url);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setContentText("Veuillez entrer un lieu d'abord !");
            alert.show();
        }
    }

    @FXML
    void sauvegarder(ActionEvent event) {
        try {
            String titre = tfTitre.getText();
            String description = taDescription.getText();
            TypeEvenement type = cbType.getValue();
            LocalDateTime dateDebut = LocalDateTime.parse(tfDateDebut.getText());
            LocalDateTime dateFin = LocalDateTime.parse(tfDateFin.getText());
            String lieu = tfLieu.getText();
            int nbMax = Integer.parseInt(tfNbMax.getText().trim());
            String statut = cbStatut.getValue();
            String image = tfImage.getText();

            if (evenementAModifier == null) {
                // AJOUTER
                Evenement e = new Evenement(titre, description, type.getId(),
                        dateDebut, dateFin, lieu, nbMax, statut);
                e.setImage(image);
                service.add(e);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Succès");
                alert.setContentText("Événement ajouté avec succès !");
                alert.show();
            } else {
                // MODIFIER
                evenementAModifier.setTitre(titre);
                evenementAModifier.setDescription(description);
                evenementAModifier.setTypeId(type.getId());
                evenementAModifier.setDateDebut(dateDebut);
                evenementAModifier.setDateFin(dateFin);
                evenementAModifier.setLieu(lieu);
                evenementAModifier.setNbParticipantsMax(nbMax);
                evenementAModifier.setStatut(statut);
                evenementAModifier.setImage(image);
                service.update(evenementAModifier);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Succès");
                alert.setContentText("Événement modifié avec succès !");
                alert.show();
            }
            retour(event);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Vérifiez les champs ! " + e.getMessage());
            alert.show();
        }
    }

    @FXML
    void retour(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GestionEvenement.fxml"));
            tfTitre.getScene().setRoot(root);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
