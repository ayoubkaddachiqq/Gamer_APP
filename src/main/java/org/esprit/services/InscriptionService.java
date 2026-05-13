package org.esprit.services;

import org.esprit.interfaces.IService;
import org.esprit.models.Inscription;
import org.esprit.utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InscriptionService implements IService<Inscription> {

    private Connection connection;

    public InscriptionService() {
        connection = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void add(Inscription i) {
        String sql = "INSERT INTO inscription (evenement_id, utilisateur_id, statut) VALUES (?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, i.getEvenementId());
            ps.setInt(2, i.getUtilisateurId());
            ps.setString(3, i.getStatut());
            ps.executeUpdate();
            System.out.println("Inscription ajoutée !");
        } catch (SQLException ex) {
            System.out.println("Erreur ajout : " + ex.getMessage());
        }
    }

    @Override
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
                i.setUtilisateurId(rs.getInt("utilisateur_id"));
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

    public List<Inscription> getByUtilisateurId(int utilisateurId) {
        List<Inscription> liste = new ArrayList<>();
        String sql = "SELECT * FROM inscription WHERE utilisateur_id=? ORDER BY date_inscription DESC";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, utilisateurId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Inscription i = new Inscription();
                i.setId(rs.getInt("id"));
                i.setEvenementId(rs.getInt("evenement_id"));
                i.setUtilisateurId(rs.getInt("utilisateur_id"));
                i.setStatut(rs.getString("statut"));
                if (rs.getTimestamp("date_inscription") != null) {
                    i.setDateInscription(rs.getTimestamp("date_inscription").toLocalDateTime());
                }
                liste.add(i);
            }
        } catch (SQLException ex) {
            System.out.println("Erreur lecture par utilisateur : " + ex.getMessage());
        }
        return liste;
    }

    @Override
    public void update(Inscription i) {
        String sql = "UPDATE inscription SET utilisateur_id=?, statut=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, i.getUtilisateurId());
            ps.setString(2, i.getStatut());
            ps.setInt(3, i.getId());
            ps.executeUpdate();
            System.out.println("Inscription modifiée !");
        } catch (SQLException ex) {
            System.out.println("Erreur modification : " + ex.getMessage());
        }
    }

    @Override
    public void delete(Inscription i) {
        String sql = "DELETE FROM inscription WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, i.getId());
            ps.executeUpdate();
            System.out.println("Inscription supprimée !");
        } catch (SQLException ex) {
            System.out.println("Erreur suppression : " + ex.getMessage());
        }
    }
}
