package org.esprit;

import org.esprit.models.Annonce;
import org.esprit.models.Categorie;
import org.esprit.services.AnnonceService;
import org.esprit.services.CategorieService;

public class Main {
    public static void main(String[] args) throws Exception {
        AnnonceService annonceService = new AnnonceService();
        CategorieService categorieService = new CategorieService();

        // --- TEST CATEGORIE ---
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

        // Modifier
        a.setId(1);
        a.setSalaire(2000.0);
        annonceService.modifier(a);

        // Supprimer
        // annonceService.supprimer(1);

        // Rechercher
        System.out.println("\n🔍 Recherche 'CS2' :");
        annonceService.rechercher("CS2").forEach(System.out::println);
    }
}