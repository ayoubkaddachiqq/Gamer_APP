package org.esprit.models;

import java.util.Date;

public class Annonce {
    private int id;
    private String titre;
    private String description;
    private String jeu;
    private double salaire;
    private Date datePublication;
    private String statut;
    private int idCategorie;
    private String nomCategorie;
    private String imagePath;

    public Annonce() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getJeu() { return jeu; }
    public void setJeu(String jeu) { this.jeu = jeu; }
    public double getSalaire() { return salaire; }
    public void setSalaire(double salaire) { this.salaire = salaire; }
    public Date getDatePublication() { return datePublication; }
    public void setDatePublication(Date datePublication) { this.datePublication = datePublication; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public int getIdCategorie() { return idCategorie; }
    public void setIdCategorie(int idCategorie) { this.idCategorie = idCategorie; }
    public String getNomCategorie() { return nomCategorie; }
    public void setNomCategorie(String nomCategorie) { this.nomCategorie = nomCategorie; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    @Override
    public String toString() {
        return "Annonce{id=" + id + ", titre=" + titre + ", jeu=" + jeu +
                ", salaire=" + salaire + ", statut=" + statut +
                ", categorie=" + nomCategorie + "}";
    }
}
