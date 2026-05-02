package org.esprit.models;

import java.time.LocalDateTime;

public class Inscription {
    private int id;
    private int evenementId;
    private int utilisateurId;
    private LocalDateTime dateInscription;
    private String statut;

    public Inscription() {}

    public Inscription(int evenementId, int utilisateurId, String statut) {
        this.evenementId = evenementId;
        this.utilisateurId = utilisateurId;
        this.statut = statut;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEvenementId() { return evenementId; }
    public void setEvenementId(int evenementId) { this.evenementId = evenementId; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() {
        return "Inscription{id=" + id + ", evenementId=" + evenementId +
                ", utilisateurId=" + utilisateurId + ", statut='" + statut + "'}";
    }
}