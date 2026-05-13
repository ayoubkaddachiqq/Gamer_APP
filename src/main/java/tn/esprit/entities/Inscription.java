package tn.esprit.entities;

import java.util.Date;

public class Inscription {
    private int id;
    private int evenementId;
    private int utilisateurId;
    private Date dateInscription;
    private String statut;

    private String nomEvenement;
    private String nomUser;

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

    public Date getDateInscription() { return dateInscription; }
    public void setDateInscription(Date dateInscription) { this.dateInscription = dateInscription; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getNomEvenement() { return nomEvenement; }
    public void setNomEvenement(String nomEvenement) { this.nomEvenement = nomEvenement; }

    public String getNomUser() { return nomUser; }
    public void setNomUser(String nomUser) { this.nomUser = nomUser; }

    @Override
    public String toString() {
        return "Inscription{id=" + id + ", evenementId=" + evenementId +
                ", utilisateurId=" + utilisateurId + ", statut='" + statut + "'}";
    }
}
