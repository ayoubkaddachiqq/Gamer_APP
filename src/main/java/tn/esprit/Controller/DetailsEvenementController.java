package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import tn.esprit.entities.Evenement;
import tn.esprit.entities.TypeEvenement;
import tn.esprit.services.MeteoService;
import tn.esprit.services.TypeEvenementService;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class DetailsEvenementController {

    public static Evenement evenementAffiche;

    @FXML private Label lbTitre;
    @FXML private Label lbDescription;
    @FXML private Label lbType;
    @FXML private Label lbLieu;
    @FXML private Label lbDateDebut;
    @FXML private Label lbDateFin;
    @FXML private Label lbNbMax;
    @FXML private Label lbStatut;
    @FXML private Label lbWeatherIcon;
    @FXML private Label lbWeatherTitle;
    @FXML private Label lbWeatherDetails;
    @FXML private WebView mapView;
    @FXML private ImageView imageEvenement;

    private final TypeEvenementService typeService = new TypeEvenementService();
    private final MeteoService meteoService = new MeteoService();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        Evenement e = evenementAffiche;
        if (e != null) {
            lbTitre.setText(e.getTitre());
            lbDescription.setText(e.getDescription());

            List<TypeEvenement> types = null;
            try {
                types = typeService.getAll();
            } catch (Exception ignored) {}
            String libelleType = "Inconnu";
            if (types != null) {
                libelleType = types.stream()
                        .filter(t -> t.getId() == e.getTypeId())
                        .map(TypeEvenement::getLibelle)
                        .findFirst()
                        .orElse("Inconnu");
            }
            lbType.setText(libelleType);
            lbLieu.setText(e.getLieu());
            lbDateDebut.setText(e.getDateDebut() != null ? dateFormat.format(e.getDateDebut()) : "-");
            lbDateFin.setText(e.getDateFin() != null ? dateFormat.format(e.getDateFin()) : "-");
            lbNbMax.setText(String.valueOf(e.getNbParticipantsMax()));
            lbStatut.setText(e.getStatut());

            verifierMeteo(e);

            String url = "https://maps.google.com/?q=" + e.getLieu().replace(" ", "+");
            mapView.getEngine().load(url);

            if (e.getImage() != null && !e.getImage().isEmpty()) {
                try {
                    Image img;
                    if (e.getImage().startsWith("http") || e.getImage().startsWith("file:")) {
                        img = new Image(e.getImage());
                    } else {
                        File f = new File(e.getImage());
                        img = new Image(f.toURI().toString());
                    }
                    imageEvenement.setImage(img);
                } catch (Exception ex) {
                    System.out.println("Image non trouvee");
                }
            }
        }

        Rectangle clip = new Rectangle();
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        clip.widthProperty().bind(mapView.widthProperty());
        clip.heightProperty().bind(mapView.heightProperty());
        mapView.setClip(clip);

        Rectangle clipImg = new Rectangle();
        clipImg.setArcWidth(30);
        clipImg.setArcHeight(30);
        clipImg.widthProperty().bind(imageEvenement.fitWidthProperty());
        clipImg.heightProperty().bind(imageEvenement.fitHeightProperty());
        imageEvenement.setClip(clipImg);
    }

    private void verifierMeteo(Evenement evenement) {
        afficherMeteo("Recherche en cours", "Analyse du lieu et de la date...", "...");
        if (evenement.getDateDebut() != null) {
            java.time.LocalDate date = evenement.getDateDebut().toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            meteoService.chargerMeteo(evenement.getLieu(), date)
                    .thenAccept(meteo -> Platform.runLater(() ->
                            afficherMeteo(meteo.titre(), meteo.details(), meteo.icon())
                    ))
                    .exceptionally(ex -> {
                        Platform.runLater(() ->
                                afficherMeteo("Meteo indisponible", "Verifier la connexion ou reessayer", "!")
                        );
                        return null;
                    });
        }
    }

    private void afficherMeteo(String titre, String details, String icon) {
        lbWeatherTitle.setText(titre);
        lbWeatherDetails.setText(details);
        lbWeatherIcon.setText(icon);
    }

    @FXML
    private void retour(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Evenements.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Gestion des Evenements");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
