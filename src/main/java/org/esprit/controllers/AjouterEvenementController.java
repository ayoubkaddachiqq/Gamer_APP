package org.esprit.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.esprit.models.Evenement;
import org.esprit.models.TypeEvenement;
import org.esprit.services.EvenementService;
import org.esprit.services.TypeEvenementService;
import org.esprit.utils.UiEffects;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AjouterEvenementController {

    @FXML private Parent rootPane;
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
    @FXML private TextField tfImage;
    @FXML private WebView mapView;

    public static Evenement evenementAModifier = null;

    private final EvenementService service = new EvenementService();
    private final TypeEvenementService typeService = new TypeEvenementService();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final DateTimeFormatter HEURE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String HUGGING_FACE_MODEL = "sdadas/byt5-text-correction";
    private static final String HUGGING_FACE_TOKEN_ENV = "HF_API_TOKEN";
    private static final Map<String, String> CORRECTIONS = creerCorrections();
    private static final Map<String, Coordonnees> COORDONNEES_TUNISIE = creerCoordonneesTunisie();

    @FXML
    void initialize() {
        UiEffects.applyEntranceAndHover(rootPane);

        List<TypeEvenement> types = typeService.getAll();
        cbType.getItems().addAll(types);

        cbStatut.getItems().addAll("Planifi\u00e9", "En cours", "Termin\u00e9", "Annul\u00e9");
        cbStatut.setValue("Planifi\u00e9");
        dpDateDebut.setValue(LocalDate.now());
        dpDateFin.setValue(LocalDate.now());
        tfHeureDebut.setText("18:00");
        tfHeureFin.setText("22:00");

        dpDateDebut.valueProperty().addListener((observable, oldValue, newValue) -> verifierMeteo());
        tfLieu.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused) {
                verifierMeteo();
            }
        });

        if (evenementAModifier != null) {
            tfTitre.setText(evenementAModifier.getTitre());
            taDescription.setText(evenementAModifier.getDescription());
            dpDateDebut.setValue(evenementAModifier.getDateDebut().toLocalDate());
            dpDateFin.setValue(evenementAModifier.getDateFin().toLocalDate());
            tfHeureDebut.setText(evenementAModifier.getDateDebut().toLocalTime().format(HEURE_FORMATTER));
            tfHeureFin.setText(evenementAModifier.getDateFin().toLocalTime().format(HEURE_FORMATTER));
            tfLieu.setText(evenementAModifier.getLieu());
            tfNbMax.setText(String.valueOf(evenementAModifier.getNbParticipantsMax()));
            cbStatut.setValue(evenementAModifier.getStatut());
            types.stream()
                    .filter(type -> type.getId() == evenementAModifier.getTypeId())
                    .findFirst()
                    .ifPresent(cbType::setValue);
            if (evenementAModifier.getImage() != null) {
                tfImage.setText(evenementAModifier.getImage());
            }
            verifierMeteo();
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
    void corrigerDescription(ActionEvent event) {
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
        afficherInfo("Correction", corrige.equals(original)
                ? "Aucune correction trouvee."
                : fallbackLocal
                ? "Correction locale appliquee. Hugging Face est indisponible ou non configure."
                : "Description corrigee avec Hugging Face.");
    }

    private CompletableFuture<String> corrigerTexteHuggingFace(String texte) {
        String apiUrl = "https://api-inference.huggingface.co/models/" + HUGGING_FACE_MODEL;
        String token = System.getenv(HUGGING_FACE_TOKEN_ENV);
        String payload = "{"
                + "\"inputs\":\"<fr> " + echapperJson(texte) + "\","
                + "\"parameters\":{\"max_length\":512},"
                + "\"options\":{\"wait_for_model\":true}"
                + "}";

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8));
        if (token != null && !token.isBlank()) {
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
            if (suivant == 'n') resultat.append('\n');
            else if (suivant == 'r') resultat.append('\r');
            else if (suivant == 't') resultat.append('\t');
            else if (suivant == '"' || suivant == '\\' || suivant == '/') resultat.append(suivant);
            else if (suivant == 'u' && i + 4 < texte.length()) {
                String hex = texte.substring(i + 1, i + 5);
                try {
                    resultat.append((char) Integer.parseInt(hex, 16));
                    i += 4;
                } catch (NumberFormatException ex) {
                    resultat.append("\\u").append(hex);
                    i += 4;
                }
            } else {
                resultat.append(suivant);
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
        if (exact != null) {
            return appliquerCasse(mot, exact);
        }

        String meilleurMot = null;
        int meilleureDistance = Integer.MAX_VALUE;
        for (Map.Entry<String, String> correction : CORRECTIONS.entrySet()) {
            String candidat = correction.getKey();
            if (Math.abs(normalise.length() - candidat.length()) > 2) {
                continue;
            }
            int distance = distanceLevenshtein(normalise, candidat);
            if (distance < meilleureDistance) {
                meilleureDistance = distance;
                meilleurMot = correction.getValue();
            }
        }

        if (meilleurMot != null && meilleureDistance <= 2) {
            return appliquerCasse(mot, meilleurMot);
        }
        return mot;
    }

    private static Map<String, String> creerCorrections() {
        Map<String, String> corrections = new LinkedHashMap<>();
        corrections.put("evenement", "\u00e9v\u00e9nement");
        corrections.put("evenements", "\u00e9v\u00e9nements");
        corrections.put("evenemen", "\u00e9v\u00e9nement");
        corrections.put("evenemeny", "\u00e9v\u00e9nement");
        corrections.put("evenemnt", "\u00e9v\u00e9nement");
        corrections.put("evenment", "\u00e9v\u00e9nement");
        corrections.put("evnement", "\u00e9v\u00e9nement");
        corrections.put("evnements", "\u00e9v\u00e9nements");
        corrections.put("entrainement", "entra\u00eenement");
        corrections.put("entrainements", "entra\u00eenements");
        corrections.put("competition", "comp\u00e9tition");
        corrections.put("competitions", "comp\u00e9titions");
        corrections.put("equipe", "\u00e9quipe");
        corrections.put("equipes", "\u00e9quipes");
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
        return corrections;
    }

    private static Map<String, Coordonnees> creerCoordonneesTunisie() {
        Map<String, Coordonnees> coordonnees = new LinkedHashMap<>();
        coordonnees.put("tunis", new Coordonnees("Tunis", 36.8065, 10.1815));
        coordonnees.put("sfax", new Coordonnees("Sfax", 34.7406, 10.7603));
        coordonnees.put("sousse", new Coordonnees("Sousse", 35.8256, 10.6369));
        coordonnees.put("monastir", new Coordonnees("Monastir", 35.7770, 10.8262));
        coordonnees.put("nabeul", new Coordonnees("Nabeul", 36.4561, 10.7376));
        coordonnees.put("ariana", new Coordonnees("Ariana", 36.8625, 10.1956));
        coordonnees.put("ben arous", new Coordonnees("Ben Arous", 36.7531, 10.2189));
        coordonnees.put("bizerte", new Coordonnees("Bizerte", 37.2744, 9.8739));
        coordonnees.put("gabes", new Coordonnees("Gabes", 33.8815, 10.0982));
        coordonnees.put("kairouan", new Coordonnees("Kairouan", 35.6781, 10.0963));
        return coordonnees;
    }

    private String normaliser(String mot) {
        return java.text.Normalizer.normalize(mot, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    private String appliquerCasse(String original, String corrige) {
        if (original.equals(original.toUpperCase())) {
            return corrige.toUpperCase();
        }
        if (Character.isUpperCase(original.charAt(0))) {
            return corrige.substring(0, 1).toUpperCase() + corrige.substring(1);
        }
        return corrige;
    }

    private int distanceLevenshtein(String a, String b) {
        int[][] distances = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            distances[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            distances[0][j] = j;
        }
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
            if (type == null) {
                throw new IllegalArgumentException("Veuillez choisir le type.");
            }

            LocalDateTime dateDebut = construireDateTime(dpDateDebut, tfHeureDebut);
            LocalDateTime dateFin = construireDateTime(dpDateFin, tfHeureFin);
            if (!dateFin.isAfter(dateDebut)) {
                throw new IllegalArgumentException("La date fin doit etre apres la date debut.");
            }

            String lieu = tfLieu.getText();
            int nbMax = Integer.parseInt(tfNbMax.getText().trim());
            String statut = cbStatut.getValue();
            String image = tfImage.getText();

            if (evenementAModifier == null) {
                Evenement evenement = new Evenement(titre, description, type.getId(),
                        dateDebut, dateFin, lieu, nbMax, statut);
                evenement.setImage(image);
                service.add(evenement);
                afficherInfo("Succes", "Evenement ajoute avec succes !");
            } else {
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
                afficherInfo("Succes", "Evenement modifie avec succes !");
            }
            retour(event);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Verifiez les champs ! " + e.getMessage());
            alert.show();
        }
    }

    private LocalDateTime construireDateTime(DatePicker datePicker, TextField heureField) {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            throw new IllegalArgumentException("Veuillez choisir une date.");
        }
        LocalTime heure = LocalTime.parse(heureField.getText().trim(), HEURE_FORMATTER);
        return LocalDateTime.of(date, heure);
    }

    @FXML
    void verifierMeteo(ActionEvent event) {
        verifierMeteo();
    }

    private void verifierMeteo() {
        String lieu = tfLieu.getText();
        LocalDate date = dpDateDebut.getValue();
        if (lieu == null || lieu.isBlank() || date == null) {
            afficherMeteo("Meteo non verifiee", "Choisir lieu et date debut", "--");
            return;
        }

        afficherMeteo("Recherche en cours", "Analyse du lieu et de la date...", "...");
        chargerMeteo(lieu, date).thenAccept(message ->
                Platform.runLater(() -> afficherMeteo(message))
        ).exceptionally(ex -> {
            Platform.runLater(() -> afficherMeteo("Meteo indisponible", "Verifier la connexion ou reessayer", "!"));
            return null;
        });
    }

    private void afficherMeteo(String message) {
        String[] parties = message.split("\\|", 3);
        if (parties.length == 3) {
            afficherMeteo(parties[0], parties[1], parties[2]);
        } else {
            afficherMeteo("Meteo", message, "i");
        }
    }

    private void afficherMeteo(String titre, String details, String icon) {
        lbWeatherTitle.setText(titre);
        lbWeatherDetails.setText(details);
        lbWeatherIcon.setText(icon);
    }

    private CompletableFuture<String> chargerMeteo(String lieu, LocalDate date) {
        String villeMeteo = extraireVilleMeteo(lieu);
        String ville = URLEncoder.encode(villeMeteo, StandardCharsets.UTF_8);
        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + ville + "&count=1&language=fr&format=json&countryCode=TN";
        HttpRequest geoRequest = HttpRequest.newBuilder(URI.create(geoUrl)).GET().build();

        return httpClient.sendAsync(geoRequest, HttpResponse.BodyHandlers.ofString())
                .thenCompose(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        Coordonnees fallback = trouverCoordonneesLocales(villeMeteo);
                        if (fallback != null) {
                            return chargerMeteoDepuisCoordonnees(fallback, date);
                        }
                        return CompletableFuture.completedFuture("Meteo indisponible|Service geocoding " + response.statusCode() + "|!");
                    }
                    String body = response.body();
                    Double latitude = extraireNombre(body, "\"latitude\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
                    Double longitude = extraireNombre(body, "\"longitude\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
                    String nom = extraireTexte(body, "\"name\"\\s*:\\s*\"([^\"]+)\"");
                    if (latitude == null || longitude == null) {
                        Coordonnees fallback = trouverCoordonneesLocales(villeMeteo);
                        if (fallback != null) {
                            return chargerMeteoDepuisCoordonnees(fallback, date);
                        }
                        return CompletableFuture.completedFuture("Lieu introuvable|Essayez Tunis, Sfax, Sousse ou Monastir|?");
                    }
                    return chargerMeteoDepuisCoordonnees(new Coordonnees(nom == null ? villeMeteo : nom, latitude, longitude), date);
                });
    }

    private CompletableFuture<String> chargerMeteoDepuisCoordonnees(Coordonnees coordonnees, LocalDate date) {
        String meteoUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + coordonnees.latitude
                + "&longitude=" + coordonnees.longitude
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                + "&timezone=auto&start_date=" + date + "&end_date=" + date;
        HttpRequest meteoRequest = HttpRequest.newBuilder(URI.create(meteoUrl)).GET().build();
        return httpClient.sendAsync(meteoRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(meteoResponse -> {
                    if (meteoResponse.statusCode() < 200 || meteoResponse.statusCode() >= 300) {
                        return "Meteo indisponible|Service meteo " + meteoResponse.statusCode() + "|!";
                    }
                    return formaterMeteo(meteoResponse.body(), coordonnees.nom, date);
                });
    }

    private String extraireVilleMeteo(String lieu) {
        String ville = lieu.split(",")[0].trim();
        ville = ville.replaceAll("(?i)\\b(centre ville|mall|gaming|arena|stade|salle|lac 2|lac)\\b", "").trim();
        return ville.isBlank() ? lieu.trim() : ville;
    }

    private Coordonnees trouverCoordonneesLocales(String lieu) {
        String normalise = normaliser(lieu);
        for (Map.Entry<String, Coordonnees> entry : COORDONNEES_TUNISIE.entrySet()) {
            if (normalise.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String formaterMeteo(String body, String nom, LocalDate date) {
        Double max = extraireNombre(body, "\"temperature_2m_max\"\\s*:\\s*\\[(-?\\d+(?:\\.\\d+)?)");
        Double min = extraireNombre(body, "\"temperature_2m_min\"\\s*:\\s*\\[(-?\\d+(?:\\.\\d+)?)");
        Integer code = extraireEntier(body, "\"weather_code\"\\s*:\\s*\\[(\\d+)");
        if (max == null || min == null || code == null) {
            return "Meteo indisponible|Aucune prevision pour " + date + "|!";
        }
        return (nom == null ? "Meteo" : nom) + "|" + descriptionCodeMeteo(code)
                + " - " + Math.round(min) + " / " + Math.round(max) + " C|"
                + iconCodeMeteo(code);
    }

    private Double extraireNombre(String texte, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(texte);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : null;
    }

    private Integer extraireEntier(String texte, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(texte);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private String extraireTexte(String texte, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(texte);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String descriptionCodeMeteo(int code) {
        if (code == 0) return "ciel clair";
        if (code <= 3) return "partiellement nuageux";
        if (code <= 48) return "brouillard";
        if (code <= 67) return "pluie";
        if (code <= 77) return "neige";
        if (code <= 82) return "averses";
        return "orage";
    }

    private String iconCodeMeteo(int code) {
        if (code == 0) return "☀";
        if (code <= 3) return "☁";
        if (code <= 48) return "≋";
        if (code <= 67) return "☂";
        if (code <= 77) return "*";
        if (code <= 82) return "☔";
        return "⚡";
    }

    private void afficherInfo(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setContentText(message);
        alert.show();
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

    private record Coordonnees(String nom, double latitude, double longitude) {
    }
}
