package org.esprit.services;

import org.esprit.models.Categorie;
import org.esprit.utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategorieService {
    private Connection cnx = MyDatabase.getInstance().getConnection();

    // CREATE
    public void ajouter(Categorie c) throws SQLException {
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
        String sql = "SELECT * FROM categorie";
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
    public void modifier(Categorie c) throws SQLException {
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