package org.esprit.services;

import org.esprit.models.Inscription;
import org.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InscriptionService {

    private Connection connection;

    public InscriptionService() {
        connection = MyDataBase.getInstance().getConnection();
    }

    public void ajouter(Inscription i) {
        String sql = "INSERT INTO inscription (evenement_id, nom_joueur, email, statut) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, i.getEvenementId());
            ps.setString(2, i.getNomJoueur());
            ps.setString(3, i.getEmail());
            ps.setString(4, i.getStatut());
            ps.executeUpdate();
            System.out.println("Inscription ajoutée !");
        } catch (SQLException ex) {
            System.out.println("Erreur ajout : " + ex.getMessage());
        }
    }

    public List<Inscription> getAll() {
        List<Inscription> liste = new ArrayList<>();
        String sql = "SELECT * FROM inscription";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Inscription i = new Inscription();
                i.setId(rs.getInt("id"));
                i.setEvenementId(rs.getInt("evenement_id"));
                i.setNomJoueur(rs.getString("nom_joueur"));
                i.setEmail(rs.getString("email"));
                i.setStatut(rs.getString("statut"));
                if (rs.getTimestamp("date_inscription") != null) {
                    i.setDateInscription(rs.getTimestamp("date_inscription").toLocalDateTime());
                }
                liste.add(i);
            }
        } catch (SQLException ex) {
            System.out.println("Erreur lecture : " + ex.getMessage());
        }
        return liste;
    }

    public void modifier(Inscription i) {
        String sql = "UPDATE inscription SET nom_joueur=?, email=?, statut=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, i.getNomJoueur());
            ps.setString(2, i.getEmail());
            ps.setString(3, i.getStatut());
            ps.setInt(4, i.getId());
            ps.executeUpdate();
            System.out.println("Inscription modifiée !");
        } catch (SQLException ex) {
            System.out.println("Erreur modification : " + ex.getMessage());
        }
    }

    public void supprimer(int id) {
        String sql = "DELETE FROM inscription WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Inscription supprimée !");
        } catch (SQLException ex) {
            System.out.println("Erreur suppression : " + ex.getMessage());
        }
    }
}