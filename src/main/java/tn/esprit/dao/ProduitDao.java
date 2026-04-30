package tn.esprit.dao;

import tn.esprit.model.Produit;

import java.util.List;
import java.util.Optional;

public interface ProduitDao {

    /**
     * Inserts a new produit row.
     */
    Produit createProduit(Produit produit);

    /**
     * Loads one produit by its primary key.
     */
    Optional<Produit> getProduitById(int id);

    /**
     * Returns all produits.
     */
    List<Produit> getAllProduits();

    /**
     * Updates an existing produit row.
     */
    Produit updateProduit(Produit produit);

    /**
     * Deletes one produit by id.
     */
    boolean deleteProduit(int id);

    /**
     * Updates only the stock of one produit.
     */
    boolean updateStock(int produitId, int nouveauStock);

    /**
     * Returns only active produits.
     */
    List<Produit> getProduitsActifs();
}
