// service liée :categorie
package org.esprit.services;

import org.esprit.models.Categorie;
import org.esprit.utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategorieService {
    private Connection cnx = MyDatabase.getInstance().getConnection();
    private static final String[][] CATEGORIES_PAR_DEFAUT = {
            {"Tank", "Joueur defensif"},
            {"Support", "Joueur support"},
            {"Attaquant", "Joueur offensif"},
            {"Defenseur", "Joueur defensif"},
            {"Strategiste", "Joueur tactique"},
            {"Coach", "Encadrement equipe"}
    };

    // VALIDATION
    public void valider(Categorie c) throws Exception {
        if (c.getNom() == null || c.getNom().trim().isEmpty())
            throw new Exception("❌ Le nom de la catégorie est obligatoire !");

        if (c.getNom().length() < 3)
            throw new Exception("❌ Le nom doit contenir au moins 3 caractères !");

        if (c.getNom().length() > 100)
            throw new Exception("❌ Le nom ne doit pas dépasser 100 caractères !");

        if (!c.getNom().matches("[a-zA-ZÀ-ÿ ]+"))
            throw new Exception("❌ Le nom ne doit contenir que des lettres !");

        if (c.getDescription() != null && c.getDescription().length() > 500)
            throw new Exception("❌ La description ne doit pas dépasser 500 caractères !");

        if (nomExiste(c.getNom(), c.getId()))
            throw new Exception("❌ Cette catégorie existe déjà !");
    }

    // Vérifie si le nom existe déjà (ignore l'id courant pour la modification)
    private boolean nomExiste(String nom, int idActuel) throws SQLException {
        String sql = "SELECT COUNT(*) FROM categorie WHERE nom = ? AND id != ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, nom);
        ps.setInt(2, idActuel);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1) > 0;
    }

    // CREATE
    public void ajouter(Categorie c) throws Exception {
        valider(c); // ← validation avant insertion
        String sql = "INSERT INTO categorie (nom, description) VALUES (?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, c.getNom());
        ps.setString(2, c.getDescription());
        ps.executeUpdate();
        System.out.println("✅ Catégorie ajoutée !");
    }

    // READ ALL
    public List<Categorie> getAll() throws SQLException {
        List<Categorie> list = new ArrayList<>();
        String sql = "SELECT * FROM categorie ORDER BY nom";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Categorie c = new Categorie();
            c.setId(rs.getInt("id"));
            c.setNom(rs.getString("nom"));
            c.setDescription(rs.getString("description"));
            list.add(c);
        }
        return list;
    }

    public void assurerCategoriesParDefaut() throws Exception {
        for (String[] categorie : CATEGORIES_PAR_DEFAUT) {
            if (!nomExiste(categorie[0], 0)) {
                ajouter(new Categorie(0, categorie[0], categorie[1]));
            }
        }
    }

    // READ BY ID
    public Categorie getById(int id) throws SQLException {
        String sql = "SELECT * FROM categorie WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Categorie c = new Categorie();
            c.setId(rs.getInt("id"));
            c.setNom(rs.getString("nom"));
            c.setDescription(rs.getString("description"));
            return c;
        }
        return null;
    }

    // UPDATE
    public void modifier(Categorie c) throws Exception {
        valider(c); // ← validation avant modification
        String sql = "UPDATE categorie SET nom=?, description=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, c.getNom());
        ps.setString(2, c.getDescription());
        ps.setInt(3, c.getId());
        ps.executeUpdate();
        System.out.println("✅ Catégorie modifiée !");
    }

    // DELETE
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM categorie WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("✅ Catégorie supprimée !");
    }
}
