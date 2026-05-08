package org.esprit.services;

import org.esprit.interfaces.IService;
import org.esprit.models.TypeEvenement;
import org.esprit.utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TypeEvenementService implements IService<TypeEvenement> {

    private Connection connection;
    private static final List<String> TYPES_PAR_DEFAUT = List.of(
            "Entrainement",
            "Tournoi",
            "Rencontre",
            "LAN Party",
            "Workshop",
            "Qualification",
            "Finale",
            "Streaming",
            "Networking"
    );

    public TypeEvenementService() {
        connection = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void add(TypeEvenement t) {
        String sql = "INSERT INTO type_evenement (libelle) VALUES (?)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, t.getLibelle());
            ps.executeUpdate();
            System.out.println("Type ajouté !");
        } catch (SQLException ex) {
            System.out.println("Erreur ajout : " + ex.getMessage());
        }
    }

    @Override
    public List<TypeEvenement> getAll() {
        garantirTypesParDefaut();
        return lireTypes();
    }

    public void garantirTypesParDefaut() {
        Set<String> libellesExistants = new LinkedHashSet<>();
        for (TypeEvenement type : lireTypes()) {
            libellesExistants.add(type.getLibelle().trim().toLowerCase());
        }

        for (String libelle : TYPES_PAR_DEFAUT) {
            if (!libellesExistants.contains(libelle.toLowerCase())) {
                add(new TypeEvenement(libelle));
            }
        }
    }

    private List<TypeEvenement> lireTypes() {
        List<TypeEvenement> liste = new ArrayList<>();
        String sql = "SELECT * FROM type_evenement";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                TypeEvenement t = new TypeEvenement();
                t.setId(rs.getInt("id"));
                t.setLibelle(rs.getString("libelle"));
                liste.add(t);
            }
        } catch (SQLException ex) {
            System.out.println("Erreur lecture : " + ex.getMessage());
        }
        return liste;
    }

    @Override
    public void update(TypeEvenement t) {
        String sql = "UPDATE type_evenement SET libelle=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, t.getLibelle());
            ps.setInt(2, t.getId());
            ps.executeUpdate();
            System.out.println("Type modifié !");
        } catch (SQLException ex) {
            System.out.println("Erreur modification : " + ex.getMessage());
        }
    }

    @Override
    public void delete(TypeEvenement t) {
        String sql = "DELETE FROM type_evenement WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, t.getId());
            ps.executeUpdate();
            System.out.println("Type supprimé !");
        } catch (SQLException ex) {
            System.out.println("Erreur suppression : " + ex.getMessage());
        }
    }
}
