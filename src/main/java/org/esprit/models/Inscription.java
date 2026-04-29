package org.esprit.models;

import java.time.LocalDateTime;

public class Inscription {
    private int id;
    private int evenementId;
    private String nomJoueur;
    private String email;
    private LocalDateTime dateInscription;
    private String statut;

    public Inscription() {}

    public Inscription(int evenementId, String nomJoueur, String email, String statut) {
        this.evenementId = evenementId;
        this.nomJoueur = nomJoueur;
        this.email = email;
        this.statut = statut;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEvenementId() { return evenementId; }
    public void setEvenementId(int evenementId) { this.evenementId = evenementId; }

    public String getNomJoueur() { return nomJoueur; }
    public void setNomJoueur(String nomJoueur) { this.nomJoueur = nomJoueur; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() {
        return "Inscription{id=" + id + ", evenementId=" + evenementId +
                ", nomJoueur='" + nomJoueur + "', statut='" + statut + "'}";
    }
}