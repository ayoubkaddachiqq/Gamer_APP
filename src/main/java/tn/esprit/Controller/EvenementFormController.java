package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.api.ApiConfig;
import tn.esprit.entities.Evenement;
import tn.esprit.entities.TypeEvenement;
import tn.esprit.services.EvenementService;
import tn.esprit.services.MeteoService;
import tn.esprit.services.TypeEvenementService;
import tn.esprit.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EvenementFormController {

    public static Evenement evenementToEdit;

    @FXML private TextField tfTitre;
    @FXML private TextArea taDescription;
    @FXML private ComboBox<TypeEvenement> cbType;
    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private TextField tfHeureDebut;
    @FXML private TextField tfHeureFin;
    @FXML private TextField tfLieu;
    @FXML private Label lbWeatherIcon;
    @FXML private Label lbWeatherTitle;
    @FXML private Label lbWeatherDetails;
    @FXML private TextField tfNbMax;
    @FXML private ComboBox<String> cbStatut;
    @FXML private TextField tfImagePath;
    @FXML private ImageView imagePreview;
    @FXML private WebView mapView;

    private String selectedImagePath;
    private final EvenementService evenementService = new EvenementService();
    private final TypeEvenementService typeService = new TypeEvenementService();
    private final MeteoService meteoService = new MeteoService();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final DateTimeFormatter HEURE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String HF_MODEL = "sdadas/byt5-text-correction";
    private static final Map<String, String> CORRECTIONS = creerCorrections();

    @FXML
    private void initialize() {
        cbStatut.setItems(FXCollections.observableArrayList("Planifié", "En cours", "Terminé", "Annulé"));
        dpDateDebut.setValue(LocalDate.now());
        dpDateFin.setValue(LocalDate.now());
        tfHeureDebut.setText("18:00");
        tfHeureFin.setText("22:00");

        chargerTypes();

        dpDateDebut.valueProperty().addListener((obs, old, val) -> verifierMeteoAuto());
        tfLieu.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) verifierMeteoAuto();
        });

        if (evenementToEdit != null) {
            remplirFormulaire();
            verifierMeteoAuto();
        } else {
            cbStatut.setValue("Planifié");
        }
    }

    private void chargerTypes() {
        try {
            typeService.garantirTypesParDefaut();
            List<TypeEvenement> types = typeService.getAll();
            cbType.setItems(FXCollections.observableArrayList(types));
        } catch (Exception e) {
            afficherErreur(e.getMessage());
        }
    }

    private void remplirFormulaire() {
        tfTitre.setText(evenementToEdit.getTitre());
        taDescription.setText(evenementToEdit.getDescription());

        if (evenementToEdit.getDateDebut() != null) {
            dpDateDebut.setValue(evenementToEdit.getDateDebut().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());
            tfHeureDebut.setText(evenementToEdit.getDateDebut().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalTime().format(HEURE_FORMATTER));
        }
        if (evenementToEdit.getDateFin() != null) {
            dpDateFin.setValue(evenementToEdit.getDateFin().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());
            tfHeureFin.setText(evenementToEdit.getDateFin().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalTime().format(HEURE_FORMATTER));
        }

        tfLieu.setText(evenementToEdit.getLieu());
        tfNbMax.setText(String.valueOf(evenementToEdit.getNbParticipantsMax()));
        cbStatut.setValue(evenementToEdit.getStatut());
        selectedImagePath = evenementToEdit.getImage();
        afficherImagePreview(selectedImagePath);

        for (TypeEvenement type : cbType.getItems()) {
            if (type.getId() == evenementToEdit.getTypeId()) {
                cbType.setValue(type);
                break;
            }
        }
    }

    @FXML
    private void enregistrerEvenement(ActionEvent event) {
        try {
            TypeEvenement type = cbType.getValue();
            if (type == null) throw new Exception("Veuillez choisir un type d'evenement");

            Date dateDebut = construireDate(dpDateDebut, tfHeureDebut);
            Date dateFin = construireDate(dpDateFin, tfHeureFin);
            if (!dateFin.after(dateDebut))
                throw new Exception("La date de fin doit etre apres la date de debut");

            int nbMax;
            try {
                nbMax = Integer.parseInt(tfNbMax.getText().trim());
            } catch (NumberFormatException e) {
                throw new Exception("Le nombre de participants doit etre un nombre valide");
            }

            Evenement evenement = evenementToEdit == null ? new Evenement() : evenementToEdit;
            evenement.setTitre(tfTitre.getText());
            evenement.setDescription(taDescription.getText());
            evenement.setTypeId(type.getId());
            evenement.setDateDebut(dateDebut);
            evenement.setDateFin(dateFin);
            evenement.setLieu(tfLieu.getText());
            evenement.setNbParticipantsMax(nbMax);
            evenement.setStatut(cbStatut.getValue());
            evenement.setImage(selectedImagePath);

            if (evenementToEdit == null) {
                evenement.setUserId(SessionManager.getCurrentUser().getId());
                evenementService.ajouter(evenement);
            } else {
                evenementService.modifier(evenement);
            }

            evenementToEdit = null;
            retourGestion(event);
        } catch (Exception e) {
            afficherErreur(e.getMessage());
        }
    }

    private Date construireDate(DatePicker datePicker, TextField heureField) throws Exception {
        LocalDate date = datePicker.getValue();
        if (date == null) throw new Exception("Veuillez choisir une date");
        LocalTime heure;
        try {
            heure = LocalTime.parse(heureField.getText().trim(), HEURE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new Exception("Format d'heure invalide. Utilisez HH:mm (ex: 18:00)");
        }
        return Date.from(date.atTime(heure).atZone(ZoneId.systemDefault()).toInstant());
    }

    @FXML
    private void corrigerDescription(ActionEvent event) {
        String texte = taDescription.getText();
        if (texte == null || texte.isBlank()) {
            afficherInfo("Correction", "La description est vide.");
            return;
        }

        afficherInfo("Correction", "Correction en cours...");
        corrigerTexteHuggingFace(texte).thenAccept(corrige ->
                Platform.runLater(() -> appliquerCorrection(texte, corrige, false))
        ).exceptionally(ex -> {
            String corrigeLocal = corrigerTexteLocal(texte);
            Platform.runLater(() -> appliquerCorrection(texte, corrigeLocal, true));
            return null;
        });
    }

    private void appliquerCorrection(String original, String corrige, boolean fallbackLocal) {
        if (corrige == null || corrige.isBlank()) {
            corrige = corrigerTexteLocal(original);
            fallbackLocal = true;
        }
        taDescription.setText(corrige);
        afficherInfo("Correction",
                corrige.equals(original) ? "Aucune correction trouvee."
                : fallbackLocal ? "Correction locale appliquee. Hugging Face est indisponible ou non configure."
                : "Description corrigee avec Hugging Face.");
    }

    private CompletableFuture<String> corrigerTexteHuggingFace(String texte) {
        String apiUrl = "https://api-inference.huggingface.co/models/" + HF_MODEL;
        ApiConfig config = ApiConfig.getInstance();
        String token = config.getHuggingFaceToken();
        String payload = "{"
                + "\"inputs\":\"<fr> " + echapperJson(texte) + "\","
                + "\"parameters\":{\"max_length\":512},"
                + "\"options\":{\"wait_for_model\":true}"
                + "}";

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
        if (token != null && !token.isBlank() && !token.equals("YOUR_HF_TOKEN_HERE")) {
            requestBuilder.header("Authorization", "Bearer " + token.trim());
        }

        return httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new IllegalStateException("Hugging Face HTTP " + response.statusCode());
                    }
                    String corrige = extraireGeneratedText(response.body());
                    if (corrige == null || corrige.isBlank()) {
                        throw new IllegalStateException("Reponse Hugging Face invalide");
                    }
                    return corrige.replaceFirst("^<fr>\\s*", "").trim();
                });
    }

    private String extraireGeneratedText(String body) {
        Matcher matcher = Pattern.compile("\"generated_text\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").matcher(body);
        return matcher.find() ? desechapperJson(matcher.group(1)) : null;
    }

    private String echapperJson(String texte) {
        return texte.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String desechapperJson(String texte) {
        StringBuilder resultat = new StringBuilder();
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);
            if (c != '\\' || i + 1 >= texte.length()) {
                resultat.append(c);
                continue;
            }
            char suivant = texte.charAt(++i);
            switch (suivant) {
                case 'n' -> resultat.append('\n');
                case 'r' -> resultat.append('\r');
                case 't' -> resultat.append('\t');
                case '"', '\\', '/' -> resultat.append(suivant);
                case 'u' -> {
                    if (i + 4 < texte.length()) {
                        String hex = texte.substring(i + 1, i + 5);
                        try {
                            resultat.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        } catch (NumberFormatException ex) {
                            resultat.append("\\u").append(hex);
                            i += 4;
                        }
                    }
                }
                default -> resultat.append(suivant);
            }
        }
        return resultat.toString();
    }

    private String corrigerTexteLocal(String texte) {
        String texteNettoye = texte.replaceAll("\\s+", " ").trim();
        Matcher matcher = Pattern.compile("\\p{L}+|\\d+|[^\\p{L}\\d]+").matcher(texteNettoye);
        StringBuilder resultat = new StringBuilder();
        while (matcher.find()) {
            String morceau = matcher.group();
            if (morceau.matches("\\p{L}+")) {
                resultat.append(corrigerMot(morceau));
            } else {
                resultat.append(morceau);
            }
        }
        String corrige = resultat.toString().trim();
        if (!corrige.isEmpty()) {
            corrige = corrige.substring(0, 1).toUpperCase() + corrige.substring(1);
        }
        return corrige;
    }

    private String corrigerMot(String mot) {
        String normalise = normaliser(mot);
        String exact = CORRECTIONS.get(normalise);
        if (exact != null) return appliquerCasse(mot, exact);
        String meilleurMot = null;
        int meilleureDistance = Integer.MAX_VALUE;
        for (Map.Entry<String, String> correction : CORRECTIONS.entrySet()) {
            String candidat = correction.getKey();
            if (Math.abs(normalise.length() - candidat.length()) > 2) continue;
            int distance = distanceLevenshtein(normalise, candidat);
            if (distance < meilleureDistance) {
                meilleureDistance = distance;
                meilleurMot = correction.getValue();
            }
        }
        if (meilleurMot != null && meilleureDistance <= 2) return appliquerCasse(mot, meilleurMot);
        return mot;
    }

    private String normaliser(String mot) {
        return java.text.Normalizer.normalize(mot, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    private String appliquerCasse(String original, String corrige) {
        if (original.equals(original.toUpperCase())) return corrige.toUpperCase();
        if (Character.isUpperCase(original.charAt(0)))
            return corrige.substring(0, 1).toUpperCase() + corrige.substring(1);
        return corrige;
    }

    private int distanceLevenshtein(String a, String b) {
        int[][] distances = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) distances[i][0] = i;
        for (int j = 0; j <= b.length(); j++) distances[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cout = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                distances[i][j] = Math.min(
                        Math.min(distances[i - 1][j] + 1, distances[i][j - 1] + 1),
                        distances[i - 1][j - 1] + cout
                );
            }
        }
        return distances[a.length()][b.length()];
    }

    private static Map<String, String> creerCorrections() {
        Map<String, String> corrections = new LinkedHashMap<>();
        corrections.put("evenement", "événement");
        corrections.put("evenements", "événements");
        corrections.put("evenemen", "événement");
        corrections.put("evenemnt", "événement");
        corrections.put("evenment", "événement");
        corrections.put("evnement", "événement");
        corrections.put("evnements", "événements");
        corrections.put("entrainement", "entraînement");
        corrections.put("entrainements", "entraînements");
        corrections.put("competition", "compétition");
        corrections.put("equipe", "équipe");
        corrections.put("equipes", "équipes");
        corrections.put("tournoi", "tournoi");
        corrections.put("tournois", "tournoi");
        corrections.put("participant", "participant");
        corrections.put("participants", "participants");
        corrections.put("inscription", "inscription");
        corrections.put("inscriptions", "inscriptions");
        corrections.put("joueur", "joueur");
        corrections.put("joueurs", "joueurs");
        corrections.put("match", "match");
        corrections.put("matches", "matchs");
        corrections.put("matchs", "matchs");
        corrections.put("session", "session");
        corrections.put("sesion", "session");
        corrections.put("nocturne", "nocturne");
        corrections.put("nocturn", "nocturne");
        corrections.put("entree", "entrée");
        corrections.put("entrer", "entrée");
        corrections.put("libre", "libre");
        corrections.put("premiere", "première");
        corrections.put("premieres", "premières");
        corrections.put("prix", "prix");
        corrections.put("inscrire", "inscrire");
        corrections.put("desinscrire", "désinscrire");
        corrections.put("annuler", "annuler");
        corrections.put("confirmer", "confirmer");
        corrections.put("supprimer", "supprimer");
        corrections.put("modifier", "modifier");
        corrections.put("enregistrer", "enregistrer");
        corrections.put("sauvegarder", "sauvegarder");
        corrections.put("capacite", "capacité");
        corrections.put("capacites", "capacités");
        corrections.put("lieu", "lieu");
        corrections.put("adresse", "adresse");
        corrections.put("online", "online");
        corrections.put("en ligne", "en ligne");
        corrections.put("presentiel", "présentiel");
        corrections.put("distanciel", "distanciel");
        return corrections;
    }

    @FXML
    private void ouvrirMaps(ActionEvent event) {
        String lieu = tfLieu.getText();
        if (!lieu.isEmpty()) {
            String url = "https://maps.google.com/?q=" + lieu.replace(" ", "+");
            mapView.getEngine().load(url);
        } else {
            afficherErreur("Veuillez entrer un lieu d'abord !");
        }
    }

    @FXML
    private void verifierMeteo(ActionEvent event) {
        verifierMeteoAuto();
    }

    private void verifierMeteoAuto() {
        String lieu = tfLieu.getText();
        LocalDate date = dpDateDebut.getValue();
        if (lieu == null || lieu.isBlank() || date == null) {
            afficherMeteo("Meteo non verifiee", "Choisir lieu et date debut", "--");
            return;
        }
        afficherMeteo("Recherche en cours", "Analyse du lieu et de la date...", "...");
        meteoService.chargerMeteo(lieu, date).thenAccept(meteo ->
                Platform.runLater(() -> afficherMeteo(meteo.titre(), meteo.details(), meteo.icon()))
        ).exceptionally(ex -> {
            Platform.runLater(() -> afficherMeteo("Meteo indisponible", "Verifier la connexion ou reessayer", "!"));
            return null;
        });
    }

    private void afficherMeteo(String titre, String details, String icon) {
        lbWeatherTitle.setText(titre);
        lbWeatherDetails.setText(details);
        lbWeatherIcon.setText(icon);
    }

    @FXML
    private void annuler(ActionEvent event) {
        evenementToEdit = null;
        retourGestion(event);
    }

    @FXML
    private void choisirImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File file = fileChooser.showOpenDialog(tfTitre.getScene().getWindow());
        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            afficherImagePreview(selectedImagePath);
        }
    }

    private void afficherImagePreview(String imagePath) {
        tfImagePath.setText(imagePath == null ? "" : imagePath);
        imagePreview.setImage(null);
        if (imagePath == null || imagePath.trim().isEmpty()) return;

        if (imagePath.startsWith("/")) {
            InputStream is = getClass().getResourceAsStream(imagePath);
            if (is != null) {
                imagePreview.setImage(new Image(is, 140, 90, true, true));
                return;
            }
        }
        File file = new File(imagePath);
        if (file.exists()) {
            imagePreview.setImage(new Image(file.toURI().toString(), 140, 90, true, true));
            return;
        }
        URL resource = getClass().getResource(imagePath);
        if (resource != null) {
            imagePreview.setImage(new Image(resource.toExternalForm(), 140, 90, true, true));
        }
    }

    private void retourGestion(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Evenements.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Gestion des Evenements");
            stage.show();
        } catch (IOException e) {
            afficherErreur(e.getMessage());
        }
    }

    private void afficherInfo(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
