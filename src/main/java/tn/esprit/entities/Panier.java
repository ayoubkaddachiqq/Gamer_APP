package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Panier {

    private Integer id;
    private String reference;
    private int userId;
    private String statut;
    private BigDecimal total = BigDecimal.ZERO;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ProduitPanier> lignes = new ArrayList<>();

    public Panier() {}

    public Panier(String reference, int userId, String statut) {
        this.reference = reference;
        this.userId = userId;
        this.statut = statut;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<ProduitPanier> getLignes() { return lignes; }
    public void setLignes(List<ProduitPanier> lignes) { this.lignes = lignes; }

    @Override
    public String toString() {
        return "Panier{id=" + id + ", reference='" + reference + "', statut='" + statut + "', total=" + total + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Panier panier)) return false;
        return id != null && Objects.equals(id, panier.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
