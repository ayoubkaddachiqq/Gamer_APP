package org.esprit;

import org.esprit.models.Evenement;
import org.esprit.models.Inscription;
import org.esprit.services.EvenementService;
import org.esprit.services.InscriptionService;
import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        EvenementService service = new EvenementService();
        InscriptionService inscService = new InscriptionService();

        System.out.println("CREATE EVENEMENT");
        Evenement e1 = new Evenement(
                "Tournoi Valorant", "Tournoi mensuel Valorant", "Tournoi",
                LocalDateTime.of(2026, 5, 1, 18, 0),
                LocalDateTime.of(2026, 5, 1, 22, 0),
                "Tunis", 16, "Planifié"
        );
        service.ajouter(e1);

        Evenement e2 = new Evenement(
                "LAN Party CS2", "Session LAN locale", "LAN Party",
                LocalDateTime.of(2026, 5, 10, 14, 0),
                LocalDateTime.of(2026, 5, 10, 20, 0),
                "Sfax", 8, "Planifié"
        );
        service.ajouter(e2);

        System.out.println("READ EVENEMENT ");
        List<Evenement> liste = service.getAll();
        for (Evenement ev : liste) {
            System.out.println(ev);
        }

        System.out.println("UPDATE EVENEMENT");
        Evenement eModif = new Evenement(
                "Tournoi Valorant EDIT", "Description modifiée", "Tournoi",
                LocalDateTime.of(2026, 6, 1, 18, 0),
                LocalDateTime.of(2026, 6, 1, 22, 0),
                "Monastir", 32, "En cours"
        );
        eModif.setId(liste.get(0).getId());
        service.modifier(eModif);

        liste = service.getAll();
        for (Evenement ev : liste) {
            System.out.println(ev);
        }

        System.out.println("CREATE INSCRIPTION ");
        int evenementId = liste.get(1).getId(); // ID du 2ème événement
        Inscription insc = new Inscription(evenementId, "Ahmed Ben Ali", "ahmed@gmail.com", "En attente");
        inscService.ajouter(insc);

        System.out.println("READ INSCRIPTION ");
        List<Inscription> inscriptions = inscService.getAll();
        for (Inscription ins : inscriptions) {
            System.out.println(ins);
        }

        System.out.println("UPDATE INSCRIPTION ");
        inscriptions.get(0).setStatut("Confirmé");
        inscService.modifier(inscriptions.get(0));

        inscriptions = inscService.getAll();
        for (Inscription ins : inscriptions) {
            System.out.println(ins);
        }

        System.out.println("DELETE INSCRIPTION ");
        inscService.supprimer(inscriptions.get(0).getId());
        System.out.println("Inscriptions restantes : " + inscService.getAll().size());

        System.out.println("DELETE EVENEMENT ");
        service.supprimer(liste.get(0).getId());
        liste = service.getAll();
        System.out.println("Événements restants : " + liste.size());
        for (Evenement ev : liste) {
            System.out.println(ev);
        }
        System.out.println("RECHERCHE EVENEMENT");
        List<Evenement> resultats = service.rechercherParTitre("LAN");
        if (resultats.isEmpty()) {
            System.out.println("Aucun événement trouvé !");
        } else {
            for (Evenement ev : resultats) {
                System.out.println(ev);
            }
        }
    }

}