package tn.esprit.services;

import tn.esprit.entities.TypeEvenement;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TypeEvenementService {

    private static final List<String> TYPES_PAR_DEFAUT = List.of(
            "Entrainement", "Tournoi", "Rencontre", "LAN Party",
            "Workshop", "Qualification", "Finale", "Streaming", "Networking"
    );

    private Connection getCnx() {
        return MyDB.getInstance().getConnection();
    }

    public void valider(TypeEvenement t) throws Exception {
        if (t.getLibelle() == null || t.getLibelle().trim().isEmpty())
            throw new Exception("Le libelle est obligatoire");
        if (t.getLibelle().length() < 3)
            throw new Exception("Le libelle doit contenir au moins 3 caracteres");
        if (t.getLibelle().length() > 100)
            throw new Exception("Le libelle ne doit pas depasser 100 caracteres");
        if (libelleExiste(t.getLibelle(), t.getId()))
            throw new Exception("Ce type d'evenement existe deja");
    }

    private boolean libelleExiste(String libelle, int idActuel) throws SQLException {
        String sql = "SELECT COUNT(*) FROM type_evenement WHERE libelle = ? AND id != ?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, libelle);
            ps.setInt(2, idActuel);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void ajouter(TypeEvenement t) throws Exception {
        valider(t);
        String sql = "INSERT INTO type_evenement (libelle) VALUES (?)";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, t.getLibelle());
            ps.executeUpdate();
        }
    }

    public List<TypeEvenement> getAll() throws SQLException {
        garantirTypesParDefaut();
        return lireTypes();
    }

    public void garantirTypesParDefaut() {
        try {
            Set<String> libellesExistants = new LinkedHashSet<>();
            for (TypeEvenement type : lireTypes()) {
                libellesExistants.add(type.getLibelle().trim().toLowerCase());
            }
            for (String libelle : TYPES_PAR_DEFAUT) {
                if (!libellesExistants.contains(libelle.toLowerCase())) {
                    String sql = "INSERT IGNORE INTO type_evenement (libelle) VALUES (?)";
                    try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
                        ps.setString(1, libelle);
                        ps.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur initialisation types: " + e.getMessage());
        }
    }

    private List<TypeEvenement> lireTypes() throws SQLException {
        List<TypeEvenement> liste = new ArrayList<>();
        String sql = "SELECT * FROM type_evenement ORDER BY id";
        try (Statement st = getCnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                TypeEvenement t = new TypeEvenement();
                t.setId(rs.getInt("id"));
                t.setLibelle(rs.getString("libelle"));
                liste.add(t);
            }
        }
        return liste;
    }

    public TypeEvenement getById(int id) throws SQLException {
        String sql = "SELECT * FROM type_evenement WHERE id = ?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TypeEvenement t = new TypeEvenement();
                    t.setId(rs.getInt("id"));
                    t.setLibelle(rs.getString("libelle"));
                    return t;
                }
            }
        }
        return null;
    }

    public void modifier(TypeEvenement t) throws Exception {
        valider(t);
        String sql = "UPDATE type_evenement SET libelle=? WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, t.getLibelle());
            ps.setInt(2, t.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM type_evenement WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
