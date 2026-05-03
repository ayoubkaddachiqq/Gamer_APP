package org.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;
import org.esprit.models.Evenement;
import org.esprit.models.TypeEvenement;
import org.esprit.services.TypeEvenementService;
import org.esprit.utils.UiEffects;
import java.io.IOException;
import java.util.List;
import javafx.scene.shape.Rectangle;

public class DetailsEvenementController {

    @FXML private Parent rootPane;
    @FXML private Label lbTitre;
    @FXML private Label lbDescription;
    @FXML private Label lbType;
    @FXML private Label lbLieu;
    @FXML private Label lbDateDebut;
    @FXML private Label lbDateFin;
    @FXML private Label lbNbMax;
    @FXML private Label lbStatut;
    @FXML private WebView mapView;
    @FXML private javafx.scene.image.ImageView imageEvenement;
    private TypeEvenementService typeService = new TypeEvenementService();
    @FXML
    void initialize() {
        UiEffects.applyEntranceAndHover(rootPane);

        Evenement e = GestionEvenementController.evenementSelectionne;
        if (e != null) {
            lbTitre.setText(e.getTitre());
            lbDescription.setText(e.getDescription());
            List<TypeEvenement> types = typeService.getAll();
            String libelleType = types.stream()
                    .filter(t -> t.getId() == e.getTypeId())
                    .map(TypeEvenement::getLibelle)
                    .findFirst()
                    .orElse("Inconnu");
            lbType.setText(libelleType);
            lbLieu.setText(e.getLieu());
            lbDateDebut.setText(e.getDateDebut().toString());
            lbDateFin.setText(e.getDateFin().toString());
            lbNbMax.setText(String.valueOf(e.getNbParticipantsMax()));
            lbStatut.setText(e.getStatut());

            String url = "https://maps.google.com/?q=" + e.getLieu().replace(" ", "+");
            mapView.getEngine().load(url);
            // l'image
            if (e.getImage() != null && !e.getImage().isEmpty()) {
                try {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(
                            new java.io.File(e.getImage()).toURI().toString());
                    imageEvenement.setImage(img);
                } catch (Exception ex) {
                    System.out.println("Image non trouvée");
                }
            }
        }
        Rectangle clip = new Rectangle();
        clip.setWidth(mapView.getPrefWidth());
        clip.setHeight(mapView.getPrefHeight());
        clip.setArcWidth(30);
        clip.setArcHeight(30);

        mapView.setClip(clip);
        Rectangle clipImg = new Rectangle();
        clipImg.setArcWidth(30);
        clipImg.setArcHeight(30);

        clipImg.widthProperty().bind(imageEvenement.fitWidthProperty());
        clipImg.heightProperty().bind(imageEvenement.fitHeightProperty());
        imageEvenement.setClip(clipImg);
        imageEvenement.setStyle(
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 20, 0.5, 0, 5);"
        );
    }

    @FXML
    void retour(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/GestionEvenement.fxml"));
            lbTitre.getScene().setRoot(root);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
