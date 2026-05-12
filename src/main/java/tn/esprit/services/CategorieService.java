package tn.esprit.services;

import tn.esprit.entities.Categorie;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategorieService {

    private static final String[][] CATEGORIES_PAR_DEFAUT = {
        {"Tank", "Joueur defensif"},
        {"Support", "Joueur support"},
        {"Attaquant", "Joueur offensif"},
        {"Defenseur", "Joueur defensif"},
        {"Strategiste", "Joueur tactique"},
        {"Coach", "Encadrement equipe"}
    };

    public void valider(Categorie c) throws Exception {
        if (c.getNom() == null || c.getNom().trim().isEmpty())
            throw new Exception("Le nom de la categorie est obligatoire");
        if (c.getNom().length() < 3)
            throw new Exception("Le nom doit contenir au moins 3 caracteres");
        if (c.getNom().length() > 100)
            throw new Exception("Le nom ne doit pas depasser 100 caracteres");
        if (!c.getNom().matches("[a-zA-ZÀ-ÿ ]+"))
            throw new Exception("Le nom ne doit contenir que des lettres");
        if (c.getDescription() != null && c.getDescription().length() > 500)
            throw new Exception("La description ne doit pas depasser 500 caracteres");
        if (nomExiste(c.getNom(), c.getId()))
            throw new Exception("Cette categorie existe deja");
    }

    private boolean nomExiste(String nom, int idActuel) throws SQLException {
        String sql = "SELECT COUNT(*) FROM categorie WHERE nom = ? AND id != ?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, nom);
            ps.setInt(2, idActuel);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void ajouter(Categorie c) throws Exception {
        valider(c);
        String sql = "INSERT INTO categorie (nom, description) VALUES (?, ?)";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, c.getNom());
            ps.setString(2, c.getDescription());
            ps.executeUpdate();
        }
    }

    public List<Categorie> getAll() throws SQLException {
        List<Categorie> list = new ArrayList<>();
        String sql = "SELECT * FROM categorie ORDER BY nom";
        try (Statement st = MyDB.getInstance().getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Categorie c = new Categorie();
                c.setId(rs.getInt("id"));
                c.setNom(rs.getString("nom"));
                c.setDescription(rs.getString("description"));
                list.add(c);
            }
        }
        return list;
    }

    public void assurerCategoriesParDefaut() throws Exception {
        for (String[] cat : CATEGORIES_PAR_DEFAUT) {
            if (!nomExiste(cat[0], 0)) {
                ajouter(new Categorie(0, cat[0], cat[1]));
            }
        }
    }

    public Categorie getById(int id) throws SQLException {
        String sql = "SELECT * FROM categorie WHERE id = ?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Categorie c = new Categorie();
                    c.setId(rs.getInt("id"));
                    c.setNom(rs.getString("nom"));
                    c.setDescription(rs.getString("description"));
                    return c;
                }
            }
        }
        return null;
    }

    public void modifier(Categorie c) throws Exception {
        valider(c);
        String sql = "UPDATE categorie SET nom=?, description=? WHERE id=?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, c.getNom());
            ps.setString(2, c.getDescription());
            ps.setInt(3, c.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM categorie WHERE id=?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
