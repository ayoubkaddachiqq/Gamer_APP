package tn.esprit.services;

import tn.esprit.entities.Inscription;
import tn.esprit.utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InscriptionService {

    private Connection getCnx() {
        return MyDB.getInstance().getConnection();
    }

    public void valider(Inscription i) throws Exception {
        if (i.getEvenementId() <= 0)
            throw new Exception("Evenement invalide");
        if (i.getUtilisateurId() <= 0)
            throw new Exception("Utilisateur invalide");
        List<String> statutsValides = List.of("En attente", "Confirmé", "Annulé");
        if (i.getStatut() != null && !statutsValides.contains(i.getStatut()))
            throw new Exception("Statut invalide");
        if (estInscrit(i.getEvenementId(), i.getUtilisateurId()))
            throw new Exception("Vous etes deja inscrit a cet evenement");
    }

    public void ajouter(Inscription i) throws Exception {
        valider(i);
        String sql = "INSERT INTO inscription (evenement_id, utilisateur_id, statut) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, i.getEvenementId());
            ps.setInt(2, i.getUtilisateurId());
            ps.setString(3, i.getStatut() != null ? i.getStatut() : "En attente");
            ps.executeUpdate();
        }
    }

    public void updateStatut(int id, String statut) throws Exception {
        List<String> statutsValides = List.of("En attente", "Confirmé", "Annulé");
        if (!statutsValides.contains(statut))
            throw new Exception("Statut invalide");
        String sql = "UPDATE inscription SET statut=? WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM inscription WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Inscription> getAll() throws SQLException {
        List<Inscription> liste = new ArrayList<>();
        String sql = "SELECT i.*, e.titre AS nom_evenement, u.username AS nom_user " +
                     "FROM inscription i " +
                     "LEFT JOIN evenement e ON i.evenement_id = e.id " +
                     "LEFT JOIN users u ON i.utilisateur_id = u.id " +
                     "ORDER BY i.date_inscription DESC";
        try (Statement st = getCnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) liste.add(mapInscription(rs));
        }
        return liste;
    }

    public List<Inscription> getByEvenement(int evenementId) throws SQLException {
        List<Inscription> liste = new ArrayList<>();
        String sql = "SELECT i.*, e.titre AS nom_evenement, u.username AS nom_user " +
                     "FROM inscription i " +
                     "LEFT JOIN evenement e ON i.evenement_id = e.id " +
                     "LEFT JOIN users u ON i.utilisateur_id = u.id " +
                     "WHERE i.evenement_id = ? ORDER BY i.date_inscription DESC";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(mapInscription(rs));
            }
        }
        return liste;
    }

    public List<Inscription> getByUser(int utilisateurId) throws SQLException {
        List<Inscription> liste = new ArrayList<>();
        String sql = "SELECT i.*, e.titre AS nom_evenement, u.username AS nom_user " +
                     "FROM inscription i " +
                     "LEFT JOIN evenement e ON i.evenement_id = e.id " +
                     "LEFT JOIN users u ON i.utilisateur_id = u.id " +
                     "WHERE i.utilisateur_id = ? ORDER BY i.date_inscription DESC";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(mapInscription(rs));
            }
        }
        return liste;
    }

    public int compterInscriptions(int evenementId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM inscription WHERE evenement_id = ?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public boolean estInscrit(int evenementId, int utilisateurId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM inscription WHERE evenement_id = ? AND utilisateur_id = ?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            ps.setInt(2, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private Inscription mapInscription(ResultSet rs) throws SQLException {
        Inscription i = new Inscription();
        i.setId(rs.getInt("id"));
        i.setEvenementId(rs.getInt("evenement_id"));
        i.setUtilisateurId(rs.getInt("utilisateur_id"));
        Timestamp ts = rs.getTimestamp("date_inscription");
        if (ts != null) i.setDateInscription(new Date(ts.getTime()));
        i.setStatut(rs.getString("statut"));
        try { i.setNomEvenement(rs.getString("nom_evenement")); } catch (SQLException ignored) {}
        try { i.setNomUser(rs.getString("nom_user")); } catch (SQLException ignored) {}
        return i;
    }
}
