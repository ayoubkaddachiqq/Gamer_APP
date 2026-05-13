package tn.esprit.services;

import tn.esprit.entities.Panier;
import tn.esprit.entities.Produit;
import tn.esprit.entities.ProduitPanier;
import tn.esprit.utils.MyDB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PanierService {

    private final ProduitService produitService = new ProduitService();

    public PanierService() {}

    private Connection getCnx() {
        return MyDB.getInstance().getConnection();
    }

    private String genererReference() {
        return "PAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ─── PANIER CRUD ───

    public Panier creerPanier(Panier panier) {
        if (panier.getReference() == null || panier.getReference().isBlank())
            panier.setReference(genererReference());
        if (panier.getStatut() == null || panier.getStatut().isBlank())
            panier.setStatut("ACTIF");
        if (panier.getTotal() == null)
            panier.setTotal(BigDecimal.ZERO);

        String sql = "INSERT INTO panier (reference, user_id, statut, total) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getCnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, panier.getReference());
            ps.setInt(2, panier.getUserId());
            ps.setString(3, panier.getStatut());
            ps.setBigDecimal(4, panier.getTotal());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) panier.setId(keys.getInt(1));
            }
            return panier;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la creation du panier", e);
        }
    }

    public List<Panier> getPaniersByUser(int userId) {
        List<Panier> list = new ArrayList<>();
        String sql = "SELECT * FROM panier WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapPanier(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des paniers", e);
        }
        return list;
    }

    public List<Panier> getAllPaniers() {
        List<Panier> list = new ArrayList<>();
        String sql = "SELECT * FROM panier ORDER BY created_at DESC";
        try (Statement st = getCnx().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapPanier(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des paniers", e);
        }
        return list;
    }

    public Optional<Panier> getPanierById(int id) {
        String sql = "SELECT * FROM panier WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapPanier(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement du panier", e);
        }
        return Optional.empty();
    }

    public void supprimerPanier(int id) {
        String sql = "DELETE FROM panier WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du panier", e);
        }
    }

    // ─── PRODUIT_PANIER OPERATIONS ───

    public void ajouterProduit(int panierId, int produitId, int quantite) {
        if (quantite <= 0) throw new IllegalArgumentException("La quantite doit etre > 0");
        Optional<Panier> panierOpt = getPanierById(panierId);
        if (panierOpt.isEmpty()) throw new IllegalArgumentException("Panier introuvable");
        Produit produit = produitService.getById(produitId)
                .orElseThrow(() -> new IllegalArgumentException("Produit introuvable"));
        if (!produit.isActif()) throw new IllegalArgumentException("Produit inactif");
        if (produit.getStock() < quantite)
            throw new IllegalArgumentException("Stock insuffisant");

        Optional<ProduitPanier> existant = getProduitDansPanier(panierId, produitId);
        if (existant.isPresent()) {
            int nouvelleQuantite = existant.get().getQuantite() + quantite;
            if (nouvelleQuantite > produit.getStock())
                throw new IllegalArgumentException("Stock insuffisant");
            BigDecimal sousTotal = existant.get().getPrixUnitaire().multiply(BigDecimal.valueOf(nouvelleQuantite));
            mettreAJourQuantite(panierId, produitId, nouvelleQuantite, existant.get().getPrixUnitaire(), sousTotal);
        } else {
            BigDecimal prixUnitaire = produit.getPrix();
            BigDecimal sousTotal = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
            String sql = "INSERT INTO produit_panier (panier_id, produit_id, quantite, prix_unitaire, sous_total) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
                ps.setInt(1, panierId);
                ps.setInt(2, produitId);
                ps.setInt(3, quantite);
                ps.setBigDecimal(4, prixUnitaire);
                ps.setBigDecimal(5, sousTotal);
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Erreur lors de l'ajout au panier", e);
            }
        }
        synchroniserTotal(panierId);
    }

    public void retirerProduit(int panierId, int produitId) {
        String sql = "DELETE FROM produit_panier WHERE panier_id=? AND produit_id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, panierId);
            ps.setInt(2, produitId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du retrait du produit", e);
        }
        synchroniserTotal(panierId);
    }

    public void mettreAJourQuantite(int panierId, int produitId, int quantite) {
        if (quantite <= 0) {
            retirerProduit(panierId, produitId);
            return;
        }
        Produit produit = produitService.getById(produitId)
                .orElseThrow(() -> new IllegalArgumentException("Produit introuvable"));
        if (produit.getStock() < quantite)
            throw new IllegalArgumentException("Stock insuffisant");

        ProduitPanier existant = getProduitDansPanier(panierId, produitId)
                .orElseThrow(() -> new IllegalArgumentException("Produit pas dans le panier"));
        BigDecimal sousTotal = existant.getPrixUnitaire().multiply(BigDecimal.valueOf(quantite));
        mettreAJourQuantite(panierId, produitId, quantite, existant.getPrixUnitaire(), sousTotal);
        synchroniserTotal(panierId);
    }

    private void mettreAJourQuantite(int panierId, int produitId, int quantite, BigDecimal prixUnitaire, BigDecimal sousTotal) {
        String sql = "UPDATE produit_panier SET quantite=?, prix_unitaire=?, sous_total=? WHERE panier_id=? AND produit_id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, quantite);
            ps.setBigDecimal(2, prixUnitaire);
            ps.setBigDecimal(3, sousTotal);
            ps.setInt(4, panierId);
            ps.setInt(5, produitId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise a jour de la quantite", e);
        }
    }

    public void viderPanier(int panierId) {
        String sql = "DELETE FROM produit_panier WHERE panier_id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, panierId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du vidage du panier", e);
        }
        mettreAJourTotal(panierId, BigDecimal.ZERO);
    }

    public Panier getPanierDetails(int panierId) {
        Panier panier = getPanierById(panierId)
                .orElseThrow(() -> new IllegalArgumentException("Panier introuvable"));
        panier.setLignes(getProduitsByPanier(panierId));
        panier.setTotal(calculerTotal(panierId));
        return panier;
    }

    public List<ProduitPanier> getProduitsByPanier(int panierId) {
        List<ProduitPanier> list = new ArrayList<>();
        String sql = """
            SELECT pp.*, p.id AS p_id, p.nom, p.description, p.prix, p.stock, p.categorie, p.actif, p.image_path,
                   p.created_at AS p_created_at, p.updated_at AS p_updated_at
            FROM produit_panier pp
            INNER JOIN produit p ON p.id = pp.produit_id
            WHERE pp.panier_id = ?
            ORDER BY pp.added_at
            """;
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, panierId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapProduitPanier(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement des produits du panier", e);
        }
        return list;
    }

    private Optional<ProduitPanier> getProduitDansPanier(int panierId, int produitId) {
        String sql = """
            SELECT pp.*, p.id AS p_id, p.nom, p.description, p.prix, p.stock, p.categorie, p.actif, p.image_path,
                   p.created_at AS p_created_at, p.updated_at AS p_updated_at
            FROM produit_panier pp
            INNER JOIN produit p ON p.id = pp.produit_id
            WHERE pp.panier_id = ? AND pp.produit_id = ?
            """;
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, panierId);
            ps.setInt(2, produitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapProduitPanier(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du chargement du produit dans le panier", e);
        }
        return Optional.empty();
    }

    public BigDecimal calculerTotal(int panierId) {
        BigDecimal total = getProduitsByPanier(panierId).stream()
                .map(ProduitPanier::getSousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        mettreAJourTotal(panierId, total);
        return total;
    }

    private void synchroniserTotal(int panierId) {
        calculerTotal(panierId);
    }

    private void mettreAJourTotal(int panierId, BigDecimal total) {
        String sql = "UPDATE panier SET total=? WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setBigDecimal(1, total);
            ps.setInt(2, panierId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise a jour du total", e);
        }
    }

    // ─── MAPPERS ───

    private Panier mapPanier(ResultSet rs) throws SQLException {
        Panier p = new Panier();
        p.setId(rs.getInt("id"));
        p.setReference(rs.getString("reference"));
        p.setUserId(rs.getInt("user_id"));
        p.setStatut(rs.getString("statut"));
        p.setTotal(rs.getBigDecimal("total"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) p.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) p.setUpdatedAt(ua.toLocalDateTime());
        return p;
    }

    private ProduitPanier mapProduitPanier(ResultSet rs) throws SQLException {
        ProduitPanier pp = new ProduitPanier();
        pp.setPanierId(rs.getInt("panier_id"));
        pp.setProduitId(rs.getInt("produit_id"));
        pp.setQuantite(rs.getInt("quantite"));
        pp.setPrixUnitaire(rs.getBigDecimal("prix_unitaire"));
        pp.setSousTotal(rs.getBigDecimal("sous_total"));
        Timestamp aa = rs.getTimestamp("added_at");
        if (aa != null) pp.setAddedAt(aa.toLocalDateTime());

        Produit produit = new Produit();
        produit.setId(rs.getInt("p_id"));
        produit.setNom(rs.getString("nom"));
        produit.setDescription(rs.getString("description"));
        produit.setPrix(rs.getBigDecimal("prix"));
        produit.setStock(rs.getInt("stock"));
        produit.setCategorie(rs.getString("categorie"));
        produit.setImagePath(rs.getString("image_path"));
        produit.setActif(rs.getBoolean("actif"));
        Timestamp pca = rs.getTimestamp("p_created_at");
        if (pca != null) produit.setCreatedAt(pca.toLocalDateTime());
        Timestamp pua = rs.getTimestamp("p_updated_at");
        if (pua != null) produit.setUpdatedAt(pua.toLocalDateTime());

        pp.setProduit(produit);
        return pp;
    }
}
