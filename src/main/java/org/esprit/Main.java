package org.esprit;

import org.esprit.models.Evenement;
import org.esprit.models.Inscription;
import org.esprit.models.TypeEvenement;
import org.esprit.services.EvenementService;
import org.esprit.services.InscriptionService;
import org.esprit.services.TypeEvenementService;

import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        EvenementService service = new EvenementService();
        InscriptionService inscService = new InscriptionService();
        TypeEvenementService typeService = new TypeEvenementService();

        System.out.println("===== CREATE TYPE =====");
        TypeEvenement t1 = new TypeEvenement("Tournoi");
        TypeEvenement t2 = new TypeEvenement("LAN Party");
        typeService.add(t1);
        typeService.add(t2);

        System.out.println("===== READ TYPE =====");
        List<TypeEvenement> types = typeService.getAll();
        for (TypeEvenement t : types) {
            System.out.println(t);
        }

        System.out.println("===== UPDATE TYPE =====");
        types.get(0).setLibelle("Tournoi Pro");
        typeService.update(types.get(0));
        types = typeService.getAll();
        for (TypeEvenement t : types) {
            System.out.println(t);
        }

        System.out.println("===== CREATE EVENEMENT =====");
        Evenement e1 = new Evenement(
                "Tournoi Valorant", "Tournoi mensuel Valorant", 1,
                LocalDateTime.of(2026, 5, 1, 18, 0),
                LocalDateTime.of(2026, 5, 1, 22, 0),
                "Tunis", 16, "Planifie"
        );
        service.add(e1);

        Evenement e2 = new Evenement(
                "LAN Party CS2", "Session LAN locale", 2,
                LocalDateTime.of(2026, 5, 10, 14, 0),
                LocalDateTime.of(2026, 5, 10, 20, 0),
                "Sfax", 8, "Planifie"
        );
        service.add(e2);

        System.out.println("===== READ EVENEMENT =====");
        List<Evenement> liste = service.getAll();
        for (Evenement ev : liste) {
            System.out.println(ev);
        }

        System.out.println("===== UPDATE EVENEMENT =====");
        Evenement eModif = new Evenement(
                "Tournoi Valorant EDIT", "Description modifiee", 1,
                LocalDateTime.of(2026, 6, 1, 18, 0),
                LocalDateTime.of(2026, 6, 1, 22, 0),
                "Monastir", 32, "En cours"
        );
        eModif.setId(liste.get(0).getId());
        service.update(eModif);

        liste = service.getAll();
        for (Evenement ev : liste) {
            System.out.println(ev);
        }

        System.out.println("===== CREATE INSCRIPTION =====");
        int evenementId = liste.get(1).getId();
        Inscription insc = new Inscription(evenementId, 1, "En attente");
        inscService.add(insc);

        System.out.println("===== READ INSCRIPTION =====");
        List<Inscription> inscriptions = inscService.getAll();
        for (Inscription ins : inscriptions) {
            System.out.println(ins);
        }

        System.out.println("===== UPDATE INSCRIPTION =====");
        inscriptions.get(0).setStatut("Confirme");
        inscService.update(inscriptions.get(0));

        inscriptions = inscService.getAll();
        for (Inscription ins : inscriptions) {
            System.out.println(ins);
        }

        System.out.println("===== DELETE INSCRIPTION =====");
        inscService.delete(inscriptions.get(0));
        System.out.println("Inscriptions restantes : " + inscService.getAll().size());

        System.out.println("===== DELETE EVENEMENT =====");
        service.delete(liste.get(0));
        liste = service.getAll();
        System.out.println("Evenements restants : " + liste.size());
        for (Evenement ev : liste) {
            System.out.println(ev);
        }

        System.out.println("===== RECHERCHE EVENEMENT =====");
        List<Evenement> resultats = service.rechercherParTitre("LAN");
        if (resultats.isEmpty()) {
            System.out.println("Aucun evenement trouve !");
        } else {
            for (Evenement ev : resultats) {
                System.out.println(ev);
            }
        }

        System.out.println("===== DELETE TYPE =====");
        typeService.delete(types.get(0));
        System.out.println("Types restants : " + typeService.getAll().size());
    }
}
