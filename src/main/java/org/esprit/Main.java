package org.esprit;

import org.esprit.models.Annonce;
import org.esprit.models.Categorie;
import org.esprit.services.AnnonceService;
import org.esprit.services.CategorieService;

public class Main {
    public static void main(String[] args) throws Exception {
        AnnonceService annonceService = new AnnonceService();
        CategorieService categorieService = new CategorieService();

        //___TEST CATEGORIE___
        Categorie cat = new Categorie();
        cat.setNom("Tank");
        cat.setDescription("Joueur défensif");
        categorieService.ajouter(cat);

        System.out.println("📋 Toutes les catégories :");
        categorieService.getAll().forEach(System.out::println);

        // --- TEST ANNONCE ---
        Annonce a = new Annonce();
        a.setTitre("Recherche Tank CS2");
        a.setDescription("Équipe pro cherche tank expérimenté");
        a.setJeu("CS2");
        a.setSalaire(1800.0);
        a.setStatut("OUVERTE");
        a.setIdCategorie(1);
        annonceService.ajouter(a);

        System.out.println("\n📋 Toutes les annonces :");
        annonceService.getAll().forEach(System.out::println);

        // --- MODIFIER ---
        a.setId(1);
        a.setSalaire(2000.0);
        annonceService.modifier(a);

        // --- SUPPRIMER (décommenter pour tester) ---
        // annonceService.supprimer(1);

        // --- RECHERCHER ---
        System.out.println("\n🔍 Recherche 'CS2' :");
        annonceService.rechercher("CS2").forEach(System.out::println);

        // ✅ LES TESTS SONT ICI, DANS LE MAIN
        System.out.println("\n🧪 TEST VALIDATIONS :");

        // Test titre trop court
        try {
            Annonce bad = new Annonce();
            bad.setTitre("AB");
            bad.setDescription("description valide");
            bad.setJeu("Valorant");
            bad.setSalaire(1000);
            bad.setStatut("OUVERTE");
            bad.setIdCategorie(1);
            annonceService.ajouter(bad);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // Test salaire négatif
        try {
            Annonce bad = new Annonce();
            bad.setTitre("Titre valide ici");
            bad.setDescription("description valide");
            bad.setJeu("Valorant");
            bad.setSalaire(-500);
            bad.setStatut("OUVERTE");
            bad.setIdCategorie(1);
            annonceService.ajouter(bad);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // Test statut invalide
        try {
            Annonce bad = new Annonce();
            bad.setTitre("Titre valide ici");
            bad.setDescription("description valide");
            bad.setJeu("Valorant");
            bad.setSalaire(1000);
            bad.setStatut("INVALIDE");
            bad.setIdCategorie(1);
            annonceService.ajouter(bad);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // Test catégorie doublon
        try {
            Categorie doublon = new Categorie();
            doublon.setNom("Tank");
            doublon.setDescription("test");
            categorieService.ajouter(doublon);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

}
