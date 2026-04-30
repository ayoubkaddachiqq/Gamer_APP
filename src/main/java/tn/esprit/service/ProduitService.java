package tn.esprit.service;

import tn.esprit.dao.ProduitDao;
import tn.esprit.dao.impl.ProduitDaoImpl;
import tn.esprit.model.Produit;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class ProduitService {

    private final ProduitDao produitDao;

    public ProduitService() {
        this(new ProduitDaoImpl());
    }

    public ProduitService(ProduitDao produitDao) {
        this.produitDao = produitDao;
    }

    /**
     * Validates and creates a new produit.
     */
    public Produit addProduit(Produit produit) {
        validateProduit(produit);
        return produitDao.createProduit(produit);
    }

    /**
     * Validates and updates an existing produit.
     */
    public Produit updateProduit(Produit produit) {
        if (produit.getId() == null) {
            throw new IllegalArgumentException("Produit id is required for update.");
        }
        validateProduit(produit);
        return produitDao.updateProduit(produit);
    }

    /**
     * Deletes a produit by id.
     */
    public boolean deleteProduit(int produitId) {
        return produitDao.deleteProduit(produitId);
    }

    /**
     * Returns every produit stored in the database.
     */
    public List<Produit> listProduits() {
        return produitDao.getAllProduits();
    }

    /**
     * Returns one produit by id when it exists.
     */
    public Optional<Produit> getProduitById(int produitId) {
        return produitDao.getProduitById(produitId);
    }

    /**
     * Applies business validation before persistence.
     */
    public void validateProduit(Produit produit) {
        if (produit == null) {
            throw new IllegalArgumentException("Produit is required.");
        }
        if (produit.getNom() == null || produit.getNom().isBlank()) {
            throw new IllegalArgumentException("Produit nom is required.");
        }
        if (produit.getPrix() == null || produit.getPrix().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Produit prix must be >= 0.");
        }
        if (produit.getStock() < 0) {
            throw new IllegalArgumentException("Produit stock must be >= 0.");
        }
    }

    /**
     * Checks whether stock is sufficient for the requested quantity.
     */
    public boolean checkStockAvailable(int produitId, int quantite) {
        Produit produit = produitDao.getProduitById(produitId)
                .orElseThrow(() -> new IllegalArgumentException("Produit not found for id " + produitId + "."));
        return produit.getStock() >= quantite;
    }
}
