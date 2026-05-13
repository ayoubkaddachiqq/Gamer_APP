package tn.esprit.services;

import tn.esprit.entities.Produit;
import tn.esprit.utils.MyDB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProduitService {

    public ProduitService() {}

    private Connection getCnx() {
        return MyDB.getInstance().getConnection();
    }

    public void valider(Produit p) {
        if (p.getNom() == null || p.getNom().trim().isEmpty())
            throw new IllegalArgumentException("Le nom du produit est obligatoire");
        if (p.getPrix() == null || p.getPrix().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Le prix doit etre >= 0");
        if (p.getStock() < 0)
            throw new IllegalArgumentException("Le stock doit etre >= 0");
    }

    public Produit ajouter(Produit p) {
        valider(p);
        String sql = "INSERT INTO produit (nom, description, prix, stock, categorie, image_path, actif, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getCnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getDescription());
            ps.setBigDecimal(3, p.getPrix());
            ps.setInt(4, p.getStock());
            ps.setString(5, p.getCategorie());
            ps.setString(6, p.getImagePath());
            ps.setBoolean(7, p.isActif());
            ps.setInt(8, p.getUserId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }
            return p;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du produit", e);
        }
    }

    public void modifier(Produit p) {
        valider(p);
        String sql = "UPDATE produit SET nom=?, description=?, prix=?, stock=?, categorie=?, image_path=?, actif=? WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getDescription());
            ps.setBigDecimal(3, p.getPrix());
            ps.setInt(4, p.getStock());
            ps.setString(5, p.getCategorie());
            ps.setString(6, p.getImagePath());
            ps.setBoolean(7, p.isActif());
            ps.setInt(8, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la modification du produit", e);
        }
    }

    public void supprimer(int id) {
        String sql = "DELETE FROM produit WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du produit", e);
        }
    }

    public List<Produit> getAll() {
        List<Produit> list = new ArrayList<>();
        String sql = "SELECT * FROM produit ORDER BY nom";
        try (Statement st = getCnx().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapProduit(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des produits", e);
        }
        return list;
    }

    public List<Produit> getProduitsActifs() {
        List<Produit> list = new ArrayList<>();
        String sql = "SELECT * FROM produit WHERE actif = TRUE ORDER BY nom";
        try (PreparedStatement ps = getCnx().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapProduit(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des produits actifs", e);
        }
        return list;
    }

    public Optional<Produit> getById(int id) {
        String sql = "SELECT * FROM produit WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapProduit(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement du produit", e);
        }
        return Optional.empty();
    }

    public boolean checkStock(int produitId, int quantite) {
        Produit p = getById(produitId).orElseThrow(() -> new IllegalArgumentException("Produit introuvable"));
        return p.getStock() >= quantite;
    }

    public void mettreAJourStock(int produitId, int nouveauStock) {
        String sql = "UPDATE produit SET stock=? WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, nouveauStock);
            ps.setInt(2, produitId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise a jour du stock", e);
        }
    }

    private Produit mapProduit(ResultSet rs) throws SQLException {
        Produit p = new Produit();
        p.setId(rs.getInt("id"));
        p.setNom(rs.getString("nom"));
        p.setDescription(rs.getString("description"));
        p.setPrix(rs.getBigDecimal("prix"));
        p.setStock(rs.getInt("stock"));
        p.setCategorie(rs.getString("categorie"));
        p.setImagePath(rs.getString("image_path"));
        p.setActif(rs.getBoolean("actif"));
        p.setUserId(rs.getInt("user_id"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) p.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) p.setUpdatedAt(ua.toLocalDateTime());
        return p;
    }
}
