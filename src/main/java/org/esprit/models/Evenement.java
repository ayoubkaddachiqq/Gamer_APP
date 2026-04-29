package org.esprit.models;

import java.time.LocalDateTime;

public class Evenement {
    private int id;
    private String titre;
    private String description;
    private String type;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String lieu;
    private int nbParticipantsMax;
    private String statut;

    public Evenement() {}

    public Evenement(String titre, String description, String type,
                     LocalDateTime dateDebut, LocalDateTime dateFin,
                     String lieu, int nbParticipantsMax, String statut) {
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.nbParticipantsMax = nbParticipantsMax;
        this.statut = statut;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public int getNbParticipantsMax() { return nbParticipantsMax; }
    public void setNbParticipantsMax(int nbParticipantsMax) { this.nbParticipantsMax = nbParticipantsMax; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() {
        return "Evenement{id=" + id + ", titre='" + titre + "', type='" + type + "', statut='" + statut + "'}";
    }
}