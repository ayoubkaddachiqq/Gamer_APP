package tn.esprit.dao;

import tn.esprit.model.ProduitPanier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProduitPanierDao {

    /**
     * Inserts a product line into the relation table.
     */
    ProduitPanier addProduitToPanier(ProduitPanier produitPanier);

    /**
     * Updates quantity and pricing information for one panier line.
     */
    boolean updateQuantiteProduitDansPanier(int panierId, int produitId, int quantite,
                                            BigDecimal prixUnitaire, BigDecimal sousTotal);

    /**
     * Deletes one product line from a panier.
     */
    boolean removeProduitFromPanier(int panierId, int produitId);

    /**
     * Loads every line for one panier.
     */
    List<ProduitPanier> getProduitsByPanierId(int panierId);

    /**
     * Deletes all product lines for one panier.
     */
    boolean clearPanier(int panierId);

    /**
     * Checks whether a product line already exists in a panier.
     */
    boolean existsProduitInPanier(int panierId, int produitId);

    /**
     * Loads one specific product line from a panier.
     */
    Optional<ProduitPanier> getProduitDansPanier(int panierId, int produitId);
}
