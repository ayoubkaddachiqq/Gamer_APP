package tn.esprit.dao;

import tn.esprit.model.Panier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PanierDao {

    /**
     * Inserts a new panier row.
     */
    Panier createPanier(Panier panier);

    /**
     * Loads one panier by id.
     */
    Optional<Panier> getPanierById(int id);

    /**
     * Returns all paniers.
     */
    List<Panier> getAllPaniers();

    /**
     * Updates panier header data.
     */
    Panier updatePanier(Panier panier);

    /**
     * Deletes one panier by id.
     */
    boolean deletePanier(int id);

    /**
     * Synchronizes the persisted total of one panier.
     */
    boolean updateTotalPanier(int id, BigDecimal total);
}
