package org.esprit.services;

import org.esprit.interfaces.IService;
import org.esprit.models.Evenement;
import org.esprit.utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EvenementService implements IService<Evenement> {

    private Connection connection;

    public EvenementService() {
        connection = MyDataBase.getInstance().getConnection();
    }

    @Override
    public void add(Evenement e) {
        String sql = "INSERT INTO evenement (titre, description, type_id, date_debut, date_fin, lieu, nb_participants_max, statut, image) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setInt(3, e.getTypeId());
            ps.setTimestamp(4, Timestamp.valueOf(e.getDateDebut()));
            ps.setTimestamp(5, Timestamp.valueOf(e.getDateFin()));
            ps.setString(6, e.getLieu());
            ps.setInt(7, e.getNbParticipantsMax());
            ps.setString(8, e.getStatut());
            ps.setString(9, e.getImage());
            ps.executeUpdate();
            System.out.println("Événement ajouté !");
        } catch (SQLException ex) {
            System.out.println("Erreur ajout : " + ex.getMessage());
        }
    }

    @Override
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
                e.setTypeId(rs.getInt("type_id"));
                e.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
                e.setDateFin(rs.getTimestamp("date_fin").toLocalDateTime());
                e.setLieu(rs.getString("lieu"));
                e.setNbParticipantsMax(rs.getInt("nb_participants_max"));
                e.setStatut(rs.getString("statut"));
                e.setImage(rs.getString("image"));
                liste.add(e);
            }
        } catch (SQLException ex) {
            System.out.println("Erreur lecture : " + ex.getMessage());
        }
        return liste;
    }

    @Override
    public void update(Evenement e) {
        String sql = "UPDATE evenement SET titre=?, description=?, type_id=?, date_debut=?, date_fin=?, lieu=?, nb_participants_max=?, statut=?, image=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setInt(3, e.getTypeId());
            ps.setTimestamp(4, Timestamp.valueOf(e.getDateDebut()));
            ps.setTimestamp(5, Timestamp.valueOf(e.getDateFin()));
            ps.setString(6, e.getLieu());
            ps.setInt(7, e.getNbParticipantsMax());
            ps.setString(8, e.getStatut());
            ps.setString(9, e.getImage());
            ps.setInt(10, e.getId());
            ps.executeUpdate();
            System.out.println("Événement modifié !");
        } catch (SQLException ex) {
            System.out.println("Erreur modification : " + ex.getMessage());
        }
    }

    @Override
    public void delete(Evenement e) {
        String sql = "DELETE FROM evenement WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, e.getId());
            int lignesSupprimees = ps.executeUpdate();
            if (lignesSupprimees > 0) {
                renumeroterIdsEvenements();
            }
            System.out.println("Événement supprimé !");
        } catch (SQLException ex) {
            System.out.println("Erreur suppression : " + ex.getMessage());
        }
    }

    private void renumeroterIdsEvenements() throws SQLException {
        Map<Integer, Integer> nouveauxIds = new LinkedHashMap<>();
        String selectSql = "SELECT id FROM evenement ORDER BY id";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(selectSql)) {
            int nouvelId = 1;
            while (rs.next()) {
                int ancienId = rs.getInt("id");
                if (ancienId != nouvelId) {
                    nouveauxIds.put(ancienId, nouvelId);
                }
                nouvelId++;
            }
        }

        if (nouveauxIds.isEmpty()) {
            reinitialiserAutoIncrement(compterEvenements() + 1);
            return;
        }

        boolean autoCommitInitial = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            definirVerificationCleEtrangere(false);

            for (Map.Entry<Integer, Integer> entry : nouveauxIds.entrySet()) {
                int ancienId = entry.getKey();
                int nouvelIdTemporaire = -entry.getValue();
                mettreAJourIdEvenement(ancienId, nouvelIdTemporaire);
                mettreAJourIdEvenementInscription(ancienId, nouvelIdTemporaire);
            }

            for (int nouvelId : nouveauxIds.values()) {
                int nouvelIdTemporaire = -nouvelId;
                mettreAJourIdEvenement(nouvelIdTemporaire, nouvelId);
                mettreAJourIdEvenementInscription(nouvelIdTemporaire, nouvelId);
            }

            reinitialiserAutoIncrement(compterEvenements() + 1);
            definirVerificationCleEtrangere(true);
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            definirVerificationCleEtrangere(true);
            throw ex;
        } finally {
            connection.setAutoCommit(autoCommitInitial);
        }
    }

    private void mettreAJourIdEvenement(int ancienId, int nouvelId) throws SQLException {
        String sql = "UPDATE evenement SET id=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, nouvelId);
            ps.setInt(2, ancienId);
            ps.executeUpdate();
        }
    }

    private void mettreAJourIdEvenementInscription(int ancienId, int nouvelId) throws SQLException {
        String sql = "UPDATE inscription SET evenement_id=? WHERE evenement_id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, nouvelId);
            ps.setInt(2, ancienId);
            ps.executeUpdate();
        }
    }

    private int compterEvenements() throws SQLException {
        String sql = "SELECT COUNT(*) FROM evenement";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void reinitialiserAutoIncrement(int prochainId) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("ALTER TABLE evenement AUTO_INCREMENT = " + Math.max(prochainId, 1));
        }
    }

    private void definirVerificationCleEtrangere(boolean active) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS=" + (active ? "1" : "0"));
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
                e.setTypeId(rs.getInt("type_id"));
                e.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
                e.setDateFin(rs.getTimestamp("date_fin").toLocalDateTime());
                e.setLieu(rs.getString("lieu"));
                e.setNbParticipantsMax(rs.getInt("nb_participants_max"));
                e.setStatut(rs.getString("statut"));
                e.setImage(rs.getString("image"));
                liste.add(e);
            }
        } catch (SQLException ex) {
            System.out.println("Erreur recherche : " + ex.getMessage());
        }
        return liste;
    }
    public List<Evenement> rechercherParLieu(String lieu) {
        List<Evenement> liste = new ArrayList<>();
        String sql = "SELECT * FROM evenement WHERE lieu LIKE ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, "%" + lieu + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Evenement e = new Evenement();
                e.setId(rs.getInt("id"));
                e.setTitre(rs.getString("titre"));
                e.setDescription(rs.getString("description"));
                e.setTypeId(rs.getInt("type_id"));
                e.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
                e.setDateFin(rs.getTimestamp("date_fin").toLocalDateTime());
                e.setLieu(rs.getString("lieu"));
                e.setNbParticipantsMax(rs.getInt("nb_participants_max"));
                e.setStatut(rs.getString("statut"));
                e.setImage(rs.getString("image"));
                liste.add(e);
            }
        } catch (SQLException ex) {
            System.out.println("Erreur recherche lieu : " + ex.getMessage());
        }
        return liste;
    }
}
