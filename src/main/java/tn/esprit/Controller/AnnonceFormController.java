package tn.esprit.Controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import tn.esprit.entities.Annonce;
import tn.esprit.entities.Categorie;
import tn.esprit.services.AnnonceService;
import tn.esprit.services.CategorieService;
import tn.esprit.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class AnnonceFormController {

    public static Annonce annonceToEdit;

    @FXML private TextField tfTitre;
    @FXML private TextArea tfDescription;
    @FXML private TextField tfJeu;
    @FXML private TextField tfSalaire;
    @FXML private ComboBox<String> cbStatut;
    @FXML private ComboBox<Categorie> cbCategorie;
    @FXML private TextField tfImagePath;
    @FXML private ImageView imagePreview;

    private String selectedImagePath;
    private final AnnonceService annonceService = new AnnonceService();
    private final CategorieService categorieService = new CategorieService();

    private final Map<String, String> corrections = new LinkedHashMap<>();
    private final Map<String, String> correctionsPhrases = new LinkedHashMap<>();

    @FXML
    private void initialize() {
        cbStatut.setItems(FXCollections.observableArrayList("OUVERTE", "FERMEE", "EN_ATTENTE"));
        initialiserCorrections();

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
    private void enregistrerAnnonce(ActionEvent event) {
        try {
            Categorie categorie = cbCategorie.getValue();
            if (categorie == null) {
                throw new Exception("Veuillez choisir une categorie");
            }

            Annonce annonce = annonceToEdit == null ? new Annonce() : annonceToEdit;
            annonce.setTitre(tfTitre.getText());
            annonce.setDescription(tfDescription.getText());
            annonce.setJeu(tfJeu.getText());
            annonce.setSalaire(parseSalaire());
            annonce.setStatut(cbStatut.getValue());
            annonce.setIdCategorie(categorie.getId());
            annonce.setNomCategorie(categorie.getNom());
            annonce.setImagePath(selectedImagePath);

            if (annonceToEdit == null) {
                annonce.setDatePublication(new Date());
                annonce.setUserId(SessionManager.getCurrentUser().getId());
                annonceService.ajouter(annonce);
            } else {
                annonceService.modifier(annonce);
            }

            annonceToEdit = null;
            retourGestion(event);
        } catch (Exception e) {
            afficherErreur(e.getMessage());
        }
    }

    @FXML
    private void annuler(ActionEvent event) {
        annonceToEdit = null;
        retourGestion(event);
    }

    private void chargerCategories() {
        try {
            categorieService.assurerCategoriesParDefaut();
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
        selectedImagePath = annonceToEdit.getImagePath();
        afficherImagePreview(selectedImagePath);

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
            throw new Exception("Le salaire doit etre un nombre valide");
        }
    }

    @FXML
    private void choisirImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image d'annonce");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        File file = fileChooser.showOpenDialog(tfTitre.getScene().getWindow());
        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            afficherImagePreview(selectedImagePath);
        }
    }

    @FXML
    private void corrigerOrthographe() {
        String titreAvant = tfTitre.getText();
        String descriptionAvant = tfDescription.getText();
        String jeuAvant = tfJeu.getText();

        String titreApres = corrigerTexte(titreAvant);
        String descriptionApres = corrigerTexte(descriptionAvant);
        String jeuApres = corrigerTexte(jeuAvant);

        tfTitre.setText(titreApres);
        tfDescription.setText(descriptionApres);
        tfJeu.setText(jeuApres);

        if (texteIdentique(titreAvant, titreApres)
                && texteIdentique(descriptionAvant, descriptionApres)
                && texteIdentique(jeuAvant, jeuApres)) {
            afficherInformation("Aucune faute connue n'a ete trouvee");
        } else {
            afficherInformation("Correction orthographique appliquee");
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Annonces.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1300, 760));
            stage.setTitle("Team Hub - Gestion des Annonces");
            stage.show();
        } catch (IOException e) {
            afficherErreur(e.getMessage());
        }
    }

    private void initialiserCorrections() {
        correctionsPhrases.put("corriger lorthographe", "corriger l'orthographe");
        correctionsPhrases.put("corriger l horthographe", "corriger l'orthographe");
        correctionsPhrases.put("lorthographe ne fonctionee pas", "l'orthographe ne fonctionne pas");
        correctionsPhrases.put("lorthographe ne fonctionne pas", "l'orthographe ne fonctionne pas");
        correctionsPhrases.put("ne fonctionee pas", "ne fonctionne pas");
        correctionsPhrases.put("ne fonctione pas", "ne fonctionne pas");
        correctionsPhrases.put("je veut", "je veux");
        correctionsPhrases.put("je veux recruter un jouer", "je veux recruter un joueur");
        correctionsPhrases.put("on cherche un jouer", "on cherche un joueur");
        correctionsPhrases.put("on cherche une jouer", "on cherche une joueuse");
        correctionsPhrases.put("on cherhce un", "on cherche un");
        correctionsPhrases.put("on chereche un", "on cherche un");
        correctionsPhrases.put("un jouer actif", "un joueur actif");
        correctionsPhrases.put("une jouer active", "une joueuse active");
        correctionsPhrases.put("avec experiance", "avec experience");
        correctionsPhrases.put("sans experiance", "sans experience");
        correctionsPhrases.put("bon niveau", "bon niveau");

        corrections.put("jveux", "veux");
        corrections.put("veut", "veux");
        corrections.put("veuxx", "veux");
        corrections.put("lorthographe", "l'orthographe");
        corrections.put("horthographe", "orthographe");
        corrections.put("orthograf", "orthographe");
        corrections.put("orthographee", "orthographe");
        corrections.put("fonctionee", "fonctionne");
        corrections.put("fonctione", "fonctionne");
        corrections.put("fonctionneee", "fonctionne");
        corrections.put("fonctionnementt", "fonctionnement");
        corrections.put("fonctionement", "fonctionnement");
        corrections.put("correctemant", "correctement");
        corrections.put("correctemment", "correctement");
        corrections.put("corectement", "correctement");
        corrections.put("svpp", "svp");
        corrections.put("svp", "svp");
        corrections.put("annance", "annonce");
        corrections.put("annoncee", "annonce");
        corrections.put("annonnce", "annonce");
        corrections.put("anonce", "annonce");
        corrections.put("annonse", "annonce");
        corrections.put("descreption", "description");
        corrections.put("discription", "description");
        corrections.put("hortographe", "orthographe");
        corrections.put("ortographe", "orthographe");
        corrections.put("corection", "correction");
        corrections.put("corrction", "correction");
        corrections.put("corrige", "corriger");
        corrections.put("coriger", "corriger");
        corrections.put("corriger", "corriger");
        corrections.put("recrutment", "recrutement");
        corrections.put("recrutemment", "recrutement");
        corrections.put("recrutemant", "recrutement");
        corrections.put("recrute", "recrute");
        corrections.put("recruter", "recruter");
        corrections.put("recherche", "recherche");
        corrections.put("recherhce", "recherche");
        corrections.put("cherchee", "cherche");
        corrections.put("cherch", "cherche");
        corrections.put("cherhce", "cherche");
        corrections.put("chereche", "cherche");
        corrections.put("jouer", "joueur");
        corrections.put("joueurr", "joueur");
        corrections.put("jouuer", "joueur");
        corrections.put("joueu", "joueur");
        corrections.put("joueurs", "joueurs");
        corrections.put("joueusee", "joueuse");
        corrections.put("actife", "actif");
        corrections.put("actiff", "actif");
        corrections.put("activee", "active");
        corrections.put("experiance", "experience");
        corrections.put("expirience", "experience");
        corrections.put("experiencee", "experience");
        corrections.put("nivau", "niveau");
        corrections.put("niveaux", "niveau");
        corrections.put("competance", "competence");
        corrections.put("competences", "competences");
        corrections.put("serieux", "serieux");
        corrections.put("serieus", "serieux");
        corrections.put("professionel", "professionnel");
        corrections.put("professionelle", "professionnelle");
        corrections.put("salairee", "salaire");
        corrections.put("categorie", "categorie");
        corrections.put("disponnible", "disponible");
        corrections.put("urgentt", "urgent");
        corrections.put("tournois", "tournoi");
        corrections.put("equipp", "equipe");
        corrections.put("equipes", "equipes");
        corrections.put("fortnitee", "Fortnite");
        corrections.put("valorantt", "Valorant");
        corrections.put("minecraftt", "Minecraft");
    }

    private String corrigerTexte(String texte) {
        if (texte == null || texte.trim().isEmpty()) return texte;

        String texteCorrige = corrigerPhrases(texte);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[\\p{L}]+");
        java.util.regex.Matcher matcher = pattern.matcher(texteCorrige);
        StringBuffer resultat = new StringBuffer();

        while (matcher.find()) {
            String mot = matcher.group();
            String correction = trouverCorrection(mot);
            matcher.appendReplacement(resultat, java.util.regex.Matcher.quoteReplacement(correction));
        }
        matcher.appendTail(resultat);

        return resultat.toString().replaceAll("[ \\t]{2,}", " ").trim();
    }

    private String corrigerPhrases(String texte) {
        String resultat = texte;
        for (Map.Entry<String, String> entry : correctionsPhrases.entrySet()) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?iu)\\b" + java.util.regex.Pattern.quote(entry.getKey()) + "\\b");
            java.util.regex.Matcher matcher = pattern.matcher(resultat);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(buffer, java.util.regex.Matcher.quoteReplacement(appliquerCassePhrase(matcher.group(), entry.getValue())));
            }
            matcher.appendTail(buffer);
            resultat = buffer.toString();
        }
        return resultat;
    }

    private String appliquerCassePhrase(String original, String correction) {
        if (original.equals(original.toUpperCase())) return correction.toUpperCase();
        if (!original.isEmpty() && Character.isUpperCase(original.charAt(0)))
            return Character.toUpperCase(correction.charAt(0)) + correction.substring(1);
        return correction;
    }

    private String trouverCorrection(String mot) {
        String minuscule = mot.toLowerCase();
        String correction = corrections.get(minuscule);
        if (correction == null) {
            if (minuscule.length() < 7) return mot;
            int distanceMax = 1;
            int meilleureDistance = distanceMax + 1;
            for (Map.Entry<String, String> entry : corrections.entrySet()) {
                if (entry.getKey().length() < 7) continue;
                int distance = distanceLevenshtein(minuscule, entry.getKey());
                if (distance < meilleureDistance) {
                    meilleureDistance = distance;
                    correction = entry.getValue();
                }
            }
            if (meilleureDistance > distanceMax) return mot;
        }
        return appliquerCasse(mot, correction);
    }

    private String appliquerCasse(String original, String correction) {
        if (original.equals(original.toUpperCase())) return correction.toUpperCase();
        if (Character.isUpperCase(original.charAt(0)))
            return Character.toUpperCase(correction.charAt(0)) + correction.substring(1);
        return correction;
    }

    private int distanceLevenshtein(String a, String b) {
        int[] precedent = new int[b.length() + 1];
        int[] courant = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) precedent[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            courant[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cout = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                courant[j] = Math.min(Math.min(courant[j - 1] + 1, precedent[j] + 1), precedent[j - 1] + cout);
            }
            int[] temp = precedent;
            precedent = courant;
            courant = temp;
        }
        return precedent[b.length()];
    }

    private boolean texteIdentique(String avant, String apres) {
        if (avant == null) return apres == null;
        return avant.equals(apres);
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void afficherInformation(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Correction");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
