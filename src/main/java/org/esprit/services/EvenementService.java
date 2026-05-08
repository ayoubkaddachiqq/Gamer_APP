package org.esprit.services;

import org.esprit.interfaces.IService;
import org.esprit.models.Evenement;
import org.esprit.models.TypeEvenement;
import org.esprit.utils.MyDataBase;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EvenementService implements IService<Evenement> {

    private Connection connection;
    private static final int NOMBRE_MINIMUM_EVENEMENTS = 9;
    private static final List<EvenementDemo> EVENEMENTS_DEMO = List.of(
            new EvenementDemo("LAN Party CS2 - Sfax Gaming Night", "Soiree LAN autour de Counter-Strike 2 avec matchs amicaux.", "Entrainement", "Sfax, Centre Ville", 80, "Planifi\u00e9", LocalDateTime.of(2026, 5, 15, 19, 0), LocalDateTime.of(2026, 5, 15, 23, 0)),
            new EvenementDemo("Entrainement Hebdomadaire - Team Phantom", "Session d'entrainement en ligne pour preparer les prochains matchs.", "Entrainement", "Online", 10, "Planifi\u00e9", LocalDateTime.of(2026, 5, 8, 21, 0), LocalDateTime.of(2026, 5, 8, 23, 0)),
            new EvenementDemo("Gaming Meetup Tunis - Networking E-Sport", "Rencontre entre joueurs, coachs et organisateurs e-sport.", "Rencontre", "Tunis, El Menzah", 200, "Planifi\u00e9", LocalDateTime.of(2026, 5, 20, 17, 0), LocalDateTime.of(2026, 5, 20, 21, 0)),
            new EvenementDemo("FIFA Champions Cup - Sousse", "Tournoi FIFA ouvert aux joueurs solo avec phases finales.", "Tournoi", "Sousse, Arena Gaming", 64, "Planifi\u00e9", LocalDateTime.of(2026, 5, 22, 18, 0), LocalDateTime.of(2026, 5, 22, 23, 30)),
            new EvenementDemo("Valorant Night Scrims", "Scrims Valorant entre equipes locales avec debriefing tactique.", "Entrainement", "Ariana, Cyber Park", 40, "Planifi\u00e9", LocalDateTime.of(2026, 5, 24, 20, 0), LocalDateTime.of(2026, 5, 25, 0, 0)),
            new EvenementDemo("Rocket League 2v2 Challenge", "Challenge Rocket League en duo avec inscription rapide.", "Tournoi", "Monastir, Gaming Zone", 32, "Planifi\u00e9", LocalDateTime.of(2026, 5, 27, 18, 30), LocalDateTime.of(2026, 5, 27, 22, 30)),
            new EvenementDemo("Workshop Coaching E-Sport", "Atelier sur la communication, la preparation mentale et les roles en equipe.", "Workshop", "Tunis, Lac 2", 45, "Planifi\u00e9", LocalDateTime.of(2026, 5, 29, 16, 0), LocalDateTime.of(2026, 5, 29, 19, 0)),
            new EvenementDemo("League of Legends Qualifier", "Qualifications League of Legends pour les equipes universitaires.", "Qualification", "Nabeul, Campus Gaming", 100, "Planifi\u00e9", LocalDateTime.of(2026, 6, 2, 17, 0), LocalDateTime.of(2026, 6, 2, 23, 0)),
            new EvenementDemo("Fortnite Community Cup", "Cup communautaire Fortnite avec classement final et lots.", "Tournoi", "Bizerte, E-Sport Hall", 96, "Planifi\u00e9", LocalDateTime.of(2026, 6, 5, 19, 0), LocalDateTime.of(2026, 6, 5, 23, 30))
    );

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
        garantirEvenementsDemo();
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
        garantirEvenementsDemo();
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
        garantirEvenementsDemo();
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

    private void garantirEvenementsDemo() {
        try {
            int total = compterEvenements();
            if (total >= NOMBRE_MINIMUM_EVENEMENTS) {
                return;
            }

            TypeEvenementService typeService = new TypeEvenementService();
            typeService.garantirTypesParDefaut();
            Map<String, Integer> types = chargerTypesParLibelle(typeService.getAll());
            Set<String> titresExistants = chargerTitresEvenements();

            for (EvenementDemo demo : EVENEMENTS_DEMO) {
                if (total >= NOMBRE_MINIMUM_EVENEMENTS) {
                    return;
                }
                if (titresExistants.contains(demo.titre.toLowerCase())) {
                    continue;
                }

                Integer typeId = types.get(demo.type.toLowerCase());
                if (typeId == null) {
                    continue;
                }

                ajouterEvenementDemo(demo, typeId);
                titresExistants.add(demo.titre.toLowerCase());
                total++;
            }
        } catch (SQLException ex) {
            System.out.println("Erreur initialisation evenements demo : " + ex.getMessage());
        }
    }

    private Map<String, Integer> chargerTypesParLibelle(List<TypeEvenement> types) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (TypeEvenement type : types) {
            result.put(type.getLibelle().trim().toLowerCase(), type.getId());
        }
        return result;
    }

    private Set<String> chargerTitresEvenements() throws SQLException {
        Set<String> titres = new HashSet<>();
        String sql = "SELECT titre FROM evenement";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String titre = rs.getString("titre");
                if (titre != null) {
                    titres.add(titre.toLowerCase());
                }
            }
        }
        return titres;
    }

    private void ajouterEvenementDemo(EvenementDemo demo, int typeId) throws SQLException {
        String sql = "INSERT INTO evenement (titre, description, type_id, date_debut, date_fin, lieu, nb_participants_max, statut, image) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, demo.titre);
            ps.setString(2, demo.description);
            ps.setInt(3, typeId);
            ps.setTimestamp(4, Timestamp.valueOf(demo.dateDebut));
            ps.setTimestamp(5, Timestamp.valueOf(demo.dateFin));
            ps.setString(6, demo.lieu);
            ps.setInt(7, demo.nbParticipantsMax);
            ps.setString(8, demo.statut);
            ps.setString(9, null);
            ps.executeUpdate();
        }
    }

    private record EvenementDemo(
            String titre,
            String description,
            String type,
            String lieu,
            int nbParticipantsMax,
            String statut,
            LocalDateTime dateDebut,
            LocalDateTime dateFin
    ) {
    }
}
