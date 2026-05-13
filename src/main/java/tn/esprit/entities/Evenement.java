package tn.esprit.entities;

import java.util.Date;

public class Evenement {
    private int id;
    private String titre;
    private String description;
    private int typeId;
    private Date dateDebut;
    private Date dateFin;
    private String lieu;
    private int nbParticipantsMax;
    private String statut;
    private String image;
    private int userId;

    public Evenement() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getTypeId() { return typeId; }
    public void setTypeId(int typeId) { this.typeId = typeId; }

    public Date getDateDebut() { return dateDebut; }
    public void setDateDebut(Date dateDebut) { this.dateDebut = dateDebut; }

    public Date getDateFin() { return dateFin; }
    public void setDateFin(Date dateFin) { this.dateFin = dateFin; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public int getNbParticipantsMax() { return nbParticipantsMax; }
    public void setNbParticipantsMax(int nbParticipantsMax) { this.nbParticipantsMax = nbParticipantsMax; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    @Override
    public String toString() {
        return "Evenement{id=" + id + ", titre='" + titre + "', typeId=" + typeId + ", statut='" + statut + "'}";
    }
}
