package org.esprit.services;

import org.esprit.models.Evenement;
import org.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementService {

    private Connection connection;

    public EvenementService() {
        connection = MyDataBase.getInstance().getConnection();
    }

    public void ajouter(Evenement e) {
        String sql = "INSERT INTO evenement (titre, description, type, date_debut, date_fin, lieu, nb_participants_max, statut) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getType());
            ps.setTimestamp(4, Timestamp.valueOf(e.getDateDebut()));
            ps.setTimestamp(5, Timestamp.valueOf(e.getDateFin()));
            ps.setString(6, e.getLieu());
            ps.setInt(7, e.getNbParticipantsMax());
            ps.setString(8, e.getStatut());
            ps.executeUpdate();
            System.out.println("Événement ajouté !");
        } catch (SQLException ex) {
            System.out.println("Erreur ajout : " + ex.getMessage());
        }
    }

    public List<Evenement> getAll() {
        List<Evenement> liste = new ArrayList<>();
        String sql = "SELECT * FROM evenement";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Evenement e = new Evenement();
                e.setId(rs.getInt("id"));
                e.setTitre(rs.getString("titre"));
                e.setDescription(rs.getString("description"));
                e.setType(rs.getString("type"));
                e.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
                e.setDateFin(rs.getTimestamp("date_fin").toLocalDateTime());
                e.setLieu(rs.getString("lieu"));
                e.setNbParticipantsMax(rs.getInt("nb_participants_max"));
                e.setStatut(rs.getString("statut"));
                liste.add(e);
            }
        } catch (SQLException ex) {
            System.out.println("Erreur lecture : " + ex.getMessage());
        }
        return liste;
    }

    public void modifier(Evenement e) {
        String sql = "UPDATE evenement SET titre=?, description=?, type=?, date_debut=?, date_fin=?, lieu=?, nb_participants_max=?, statut=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getType());
            ps.setTimestamp(4, Timestamp.valueOf(e.getDateDebut()));
            ps.setTimestamp(5, Timestamp.valueOf(e.getDateFin()));
            ps.setString(6, e.getLieu());
            ps.setInt(7, e.getNbParticipantsMax());
            ps.setString(8, e.getStatut());
            ps.setInt(9, e.getId());
            ps.executeUpdate();
            System.out.println("Événement modifié !");
        } catch (SQLException ex) {
            System.out.println("Erreur modification : " + ex.getMessage());
        }
    }

    public void supprimer(int id) {
        String sql = "DELETE FROM evenement WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Événement supprimé !");
        } catch (SQLException ex) {
            System.out.println("Erreur suppression : " + ex.getMessage());
        }
    }
    public List<Evenement> rechercherParTitre(String titre) {
        List<Evenement> liste = new ArrayList<>();
        String sql = "SELECT * FROM evenement WHERE titre LIKE ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, "%" + titre + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Evenement e = new Evenement();
                e.setId(rs.getInt("id"));
                e.setTitre(rs.getString("titre"));
                e.setDescription(rs.getString("description"));
                e.setType(rs.getString("type"));
                e.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
                e.setDateFin(rs.getTimestamp("date_fin").toLocalDateTime());
                e.setLieu(rs.getString("lieu"));
                e.setNbParticipantsMax(rs.getInt("nb_participants_max"));
                e.setStatut(rs.getString("statut"));
                liste.add(e);
            }
        } catch (SQLException ex) {
            System.out.println("Erreur recherche : " + ex.getMessage());
        }
        return liste;
    }


}