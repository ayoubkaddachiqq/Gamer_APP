package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Produit {

    private Integer id;
    private String nom;
    private String description;
    private BigDecimal prix;
    private int stock;
    private String categorie;
    private String imagePath;
    private boolean actif;
    private int userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Produit() {}

    public Produit(String nom, String description, BigDecimal prix, int stock, String categorie, boolean actif, int userId) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.stock = stock;
        this.categorie = categorie;
        this.actif = actif;
        this.userId = userId;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Produit{id=" + id + ", nom='" + nom + "', prix=" + prix + ", stock=" + stock + ", categorie='" + categorie + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Produit produit)) return false;
        return id != null && Objects.equals(id, produit.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
