package tn.esprit.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class ProduitPanier {

    private int panierId;
    private int produitId;
    private int quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal sousTotal;
    private LocalDateTime addedAt;
    private Produit produit;

    public ProduitPanier() {
    }

    public ProduitPanier(int panierId, int produitId, int quantite, BigDecimal prixUnitaire, BigDecimal sousTotal) {
        this.panierId = panierId;
        this.produitId = produitId;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.sousTotal = sousTotal;
    }

    public ProduitPanier(int panierId, int produitId, int quantite, BigDecimal prixUnitaire,
                         BigDecimal sousTotal, LocalDateTime addedAt, Produit produit) {
        this.panierId = panierId;
        this.produitId = produitId;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.sousTotal = sousTotal;
        this.addedAt = addedAt;
        this.produit = produit;
    }

    public int getPanierId() {
        return panierId;
    }

    public void setPanierId(int panierId) {
        this.panierId = panierId;
    }

    public int getProduitId() {
        return produitId;
    }

    public void setProduitId(int produitId) {
        this.produitId = produitId;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public BigDecimal getSousTotal() {
        return sousTotal;
    }

    public void setSousTotal(BigDecimal sousTotal) {
        this.sousTotal = sousTotal;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    @Override
    public String toString() {
        String produitLabel = produit != null ? produit.getNom() : String.valueOf(produitId);
        return "ProduitPanier{" +
                "panierId=" + panierId +
                ", produit=" + produitLabel +
                ", quantite=" + quantite +
                ", prixUnitaire=" + prixUnitaire +
                ", sousTotal=" + sousTotal +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProduitPanier)) {
            return false;
        }
        ProduitPanier that = (ProduitPanier) o;
        return panierId == that.panierId && produitId == that.produitId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(panierId, produitId);
    }
}
