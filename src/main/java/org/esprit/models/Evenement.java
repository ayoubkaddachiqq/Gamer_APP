package org.esprit.models;

import java.time.LocalDateTime;

public class Evenement {
    private int id;
    private String titre;
    private String description;
    private int typeId;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String lieu;
    private double latitude;
    private double longitude;
    private int nbParticipantsMax;
    private String statut;
    private String image;

    public Evenement() {}

    public Evenement(String titre, String description, int typeId,
                     LocalDateTime dateDebut, LocalDateTime dateFin,
                     String lieu, double latitude, double longitude,
                     int nbParticipantsMax, String statut) {
        this.titre = titre;
        this.description = description;
        this.typeId = typeId;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.latitude = latitude;
        this.longitude = longitude;
        this.nbParticipantsMax = nbParticipantsMax;
        this.statut = statut;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getTypeId() { return typeId; }
    public void setTypeId(int typeId) { this.typeId = typeId; }
    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }
    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public int getNbParticipantsMax() { return nbParticipantsMax; }
    public void setNbParticipantsMax(int nbParticipantsMax) { this.nbParticipantsMax = nbParticipantsMax; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    @Override
    public String toString() {
        return "Evenement{id=" + id + ", titre='" + titre + "', typeId=" + typeId + ", statut='" + statut + "'}";
    }
}