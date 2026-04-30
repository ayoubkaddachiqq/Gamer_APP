package tn.esprit.service;

import tn.esprit.dao.PanierDao;
import tn.esprit.dao.ProduitDao;
import tn.esprit.dao.ProduitPanierDao;
import tn.esprit.dao.impl.PanierDaoImpl;
import tn.esprit.dao.impl.ProduitDaoImpl;
import tn.esprit.dao.impl.ProduitPanierDaoImpl;
import tn.esprit.model.Panier;
import tn.esprit.model.Produit;
import tn.esprit.model.ProduitPanier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class PanierService {

    private final PanierDao panierDao;
    private final ProduitDao produitDao;
    private final ProduitPanierDao produitPanierDao;

    public PanierService() {
        this(new PanierDaoImpl(), new ProduitDaoImpl(), new ProduitPanierDaoImpl());
    }

    public PanierService(PanierDao panierDao, ProduitDao produitDao, ProduitPanierDao produitPanierDao) {
        this.panierDao = panierDao;
        this.produitDao = produitDao;
        this.produitPanierDao = produitPanierDao;
    }

    /**
     * Creates a new panier with generated defaults when needed.
     */
    public Panier createPanier(Panier panier) {
        if (panier == null) {
            throw new IllegalArgumentException("Panier is required.");
        }
        if (panier.getReference() == null || panier.getReference().isBlank()) {
            panier.setReference(generateReference());
        }
        if (panier.getStatut() == null || panier.getStatut().isBlank()) {
            panier.setStatut("ACTIF");
        }
        if (panier.getTotal() == null) {
            panier.setTotal(BigDecimal.ZERO);
        }
        return panierDao.createPanier(panier);
    }

    /**
     * Returns every panier header stored in the database.
     */
    public List<Panier> listPaniers() {
        return panierDao.getAllPaniers();
    }

    /**
     * Adds a product to a panier and updates the persisted total.
     */
    public void addProduitToPanier(int panierId, int produitId, int quantite) {
        validateQuantite(quantite);
        requirePanier(panierId);
        Produit produit = requireProduit(produitId);

        if (!produit.isActif()) {
            throw new IllegalArgumentException("Produit is inactive and cannot be added.");
        }
        if (produit.getStock() < quantite) {
            throw new IllegalArgumentException("Requested quantity exceeds available stock.");
        }

        ProduitPanier existingLine = produitPanierDao.getProduitDansPanier(panierId, produitId).orElse(null);
        if (existingLine != null) {
            int nouvelleQuantite = existingLine.getQuantite() + quantite;
            if (nouvelleQuantite > produit.getStock()) {
                throw new IllegalArgumentException("Requested quantity exceeds available stock.");
            }
            BigDecimal sousTotal = existingLine.getPrixUnitaire().multiply(BigDecimal.valueOf(nouvelleQuantite));
            produitPanierDao.updateQuantiteProduitDansPanier(
                    panierId, produitId, nouvelleQuantite, existingLine.getPrixUnitaire(), sousTotal
            );
        } else {
            BigDecimal prixUnitaire = produit.getPrix();
            BigDecimal sousTotal = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
            ProduitPanier produitPanier = new ProduitPanier(panierId, produitId, quantite, prixUnitaire, sousTotal);
            produitPanierDao.addProduitToPanier(produitPanier);
        }
        synchronizeTotal(panierId);
    }

    /**
     * Removes one product line from a panier and recalculates total.
     */
    public void removeProduitFromPanier(int panierId, int produitId) {
        requirePanier(panierId);
        produitPanierDao.removeProduitFromPanier(panierId, produitId);
        synchronizeTotal(panierId);
    }

    /**
     * Updates the quantity of one panier line or removes it when quantity is zero.
     */
    public void updateQuantite(int panierId, int produitId, int quantite) {
        requirePanier(panierId);
        Produit produit = requireProduit(produitId);

        if (quantite <= 0) {
            removeProduitFromPanier(panierId, produitId);
            return;
        }
        if (produit.getStock() < quantite) {
            throw new IllegalArgumentException("Requested quantity exceeds available stock.");
        }

        ProduitPanier existingLine = produitPanierDao.getProduitDansPanier(panierId, produitId)
                .orElseThrow(() -> new IllegalArgumentException("Produit is not present in the panier."));

        BigDecimal sousTotal = existingLine.getPrixUnitaire().multiply(BigDecimal.valueOf(quantite));
        produitPanierDao.updateQuantiteProduitDansPanier(
                panierId, produitId, quantite, existingLine.getPrixUnitaire(), sousTotal
        );
        synchronizeTotal(panierId);
    }

    /**
     * Loads a panier with all of its lines and synchronized total.
     */
    public Panier getPanierDetails(int panierId) {
        Panier panier = requirePanier(panierId);
        List<ProduitPanier> lignes = produitPanierDao.getProduitsByPanierId(panierId);
        panier.setLignes(lignes);
        panier.setTotal(calculateTotal(panierId));
        return panier;
    }

    /**
     * Recalculates the panier total from its line subtotals.
     */
    public BigDecimal calculateTotal(int panierId) {
        BigDecimal total = produitPanierDao.getProduitsByPanierId(panierId).stream()
                .map(ProduitPanier::getSousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        panierDao.updateTotalPanier(panierId, total);
        return total;
    }

    /**
     * Removes all lines from a panier and resets its total.
     */
    public void clearPanier(int panierId) {
        requirePanier(panierId);
        produitPanierDao.clearPanier(panierId);
        panierDao.updateTotalPanier(panierId, BigDecimal.ZERO);
    }

    /**
     * Keeps panier.total aligned with relation-table contents.
     */
    private void synchronizeTotal(int panierId) {
        calculateTotal(panierId);
    }

    /**
     * Loads one panier or fails with a clear business message.
     */
    private Panier requirePanier(int panierId) {
        return panierDao.getPanierById(panierId)
                .orElseThrow(() -> new IllegalArgumentException("Panier not found for id " + panierId + "."));
    }

    /**
     * Loads one produit or fails with a clear business message.
     */
    private Produit requireProduit(int produitId) {
        return produitDao.getProduitById(produitId)
                .orElseThrow(() -> new IllegalArgumentException("Produit not found for id " + produitId + "."));
    }

    /**
     * Validates that quantity stays strictly positive for add operations.
     */
    private void validateQuantite(int quantite) {
        if (quantite <= 0) {
            throw new IllegalArgumentException("Quantite must be > 0.");
        }
    }

    /**
     * Generates a readable business reference for newly created paniers.
     */
    private String generateReference() {
        return "PAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
