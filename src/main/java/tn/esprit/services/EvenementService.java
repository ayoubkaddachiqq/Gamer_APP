package tn.esprit.services;

import tn.esprit.entities.Evenement;
import tn.esprit.entities.TypeEvenement;
import tn.esprit.utils.MyDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;

public class EvenementService {

    private static final int NOMBRE_MINIMUM_EVENEMENTS = 9;

    private static final Object[][] EVENEMENTS_DEMO = {
        {"LAN Party CS2 - Sfax Gaming Night", "Soiree LAN autour de Counter-Strike 2 avec matchs amicaux.", "Entrainement", "Sfax, Centre Ville", 80, "Planifié", null},
        {"Entrainement Hebdomadaire - Team Phantom", "Session d'entrainement en ligne pour preparer les prochains matchs.", "Entrainement", "Online", 10, "Planifié", null},
        {"Gaming Meetup Tunis - Networking E-Sport", "Rencontre entre joueurs, coachs et organisateurs e-sport.", "Rencontre", "Tunis, El Menzah", 200, "Planifié", null},
        {"FIFA Champions Cup - Sousse", "Tournoi FIFA ouvert aux joueurs solo avec phases finales.", "Tournoi", "Sousse, Arena Gaming", 64, "Planifié", null},
        {"Valorant Night Scrims", "Scrims Valorant entre equipes locales avec debriefing tactique.", "Entrainement", "Ariana, Cyber Park", 40, "Planifié", null},
        {"Rocket League 2v2 Challenge", "Challenge Rocket League en duo avec inscription rapide.", "Tournoi", "Monastir, Gaming Zone", 32, "Planifié", null},
        {"Workshop Coaching E-Sport", "Atelier sur la communication, la preparation mentale et les roles en equipe.", "Workshop", "Tunis, Lac 2", 45, "Planifié", null},
        {"League of Legends Qualifier", "Qualifications League of Legends pour les equipes universitaires.", "Qualification", "Nabeul, Campus Gaming", 100, "Planifié", null},
        {"Fortnite Community Cup", "Cup communautaire Fortnite avec classement final et lots.", "Tournoi", "Bizerte, E-Sport Hall", 96, "Planifié", null}
    };

    private Connection getCnx() {
        return MyDB.getInstance().getConnection();
    }

    public void valider(Evenement e) throws Exception {
        if (e.getTitre() == null || e.getTitre().trim().isEmpty())
            throw new Exception("Le titre est obligatoire");
        if (e.getTitre().length() < 5)
            throw new Exception("Le titre doit contenir au moins 5 caracteres");
        if (e.getTitre().length() > 255)
            throw new Exception("Le titre ne doit pas depasser 255 caracteres");
        if (e.getDescription() == null || e.getDescription().trim().isEmpty())
            throw new Exception("La description est obligatoire");
        if (e.getTypeId() <= 0)
            throw new Exception("Veuillez choisir un type d'evenement");
        if (e.getDateDebut() == null)
            throw new Exception("La date de debut est obligatoire");
        if (e.getDateFin() == null)
            throw new Exception("La date de fin est obligatoire");
        if (!e.getDateFin().after(e.getDateDebut()))
            throw new Exception("La date de fin doit etre apres la date de debut");
        if (e.getLieu() == null || e.getLieu().trim().isEmpty())
            throw new Exception("Le lieu est obligatoire");
        if (e.getNbParticipantsMax() <= 0)
            throw new Exception("Le nombre de participants doit etre superieur a 0");
        List<String> statutsValides = List.of("Planifié", "En cours", "Terminé", "Annulé");
        if (!statutsValides.contains(e.getStatut()))
            throw new Exception("Statut invalide. Valeurs acceptees : Planifié, En cours, Terminé, Annulé");
    }

    public void ajouter(Evenement e) throws Exception {
        valider(e);
        String sql = "INSERT INTO evenement (titre, description, type_id, date_debut, date_fin, lieu, nb_participants_max, statut, image, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setInt(3, e.getTypeId());
            ps.setTimestamp(4, new Timestamp(e.getDateDebut().getTime()));
            ps.setTimestamp(5, new Timestamp(e.getDateFin().getTime()));
            ps.setString(6, e.getLieu());
            ps.setInt(7, e.getNbParticipantsMax());
            ps.setString(8, e.getStatut());
            ps.setString(9, e.getImage());
            ps.setInt(10, e.getUserId() > 0 ? e.getUserId() : 1);
            ps.executeUpdate();
        }
    }

    public void modifier(Evenement e) throws Exception {
        valider(e);
        String sql = "UPDATE evenement SET titre=?, description=?, type_id=?, date_debut=?, date_fin=?, lieu=?, nb_participants_max=?, statut=?, image=? WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setInt(3, e.getTypeId());
            ps.setTimestamp(4, new Timestamp(e.getDateDebut().getTime()));
            ps.setTimestamp(5, new Timestamp(e.getDateFin().getTime()));
            ps.setString(6, e.getLieu());
            ps.setInt(7, e.getNbParticipantsMax());
            ps.setString(8, e.getStatut());
            ps.setString(9, e.getImage());
            ps.setInt(10, e.getId());
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM evenement WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Evenement> getAll() throws SQLException {
        garantirEvenementsDemo();
        List<Evenement> liste = new ArrayList<>();
        String sql = "SELECT * FROM evenement ORDER BY date_debut DESC";
        try (Statement st = getCnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                liste.add(mapEvenement(rs));
            }
        }
        return liste;
    }

    public Evenement getById(int id) throws SQLException {
        String sql = "SELECT * FROM evenement WHERE id = ?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapEvenement(rs);
            }
        }
        return null;
    }

    public List<Evenement> getByUserId(int userId) throws SQLException {
        List<Evenement> liste = new ArrayList<>();
        String sql = "SELECT * FROM evenement WHERE user_id = ? ORDER BY date_debut DESC";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(mapEvenement(rs));
            }
        }
        return liste;
    }

    public List<Evenement> rechercherParTitre(String titre) throws SQLException {
        garantirEvenementsDemo();
        List<Evenement> liste = new ArrayList<>();
        String sql = "SELECT * FROM evenement WHERE titre LIKE ? ORDER BY date_debut DESC";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, "%" + titre + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(mapEvenement(rs));
            }
        }
        return liste;
    }

    public List<Evenement> rechercherParLieu(String lieu) throws SQLException {
        garantirEvenementsDemo();
        List<Evenement> liste = new ArrayList<>();
        String sql = "SELECT * FROM evenement WHERE lieu LIKE ? ORDER BY date_debut DESC";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, "%" + lieu + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(mapEvenement(rs));
            }
        }
        return liste;
    }

    public List<Evenement> getUpcoming() throws SQLException {
        List<Evenement> liste = new ArrayList<>();
        String sql = "SELECT * FROM evenement WHERE date_debut >= NOW() ORDER BY date_debut ASC LIMIT 10";
        try (Statement st = getCnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) liste.add(mapEvenement(rs));
        }
        return liste;
    }

    private void garantirEvenementsDemo() {
        try {
            int total = compterEvenements();
            if (total >= NOMBRE_MINIMUM_EVENEMENTS) return;

            TypeEvenementService typeService = new TypeEvenementService();
            typeService.garantirTypesParDefaut();
            Map<String, Integer> types = chargerTypesParLibelle(typeService.getAll());
            Set<String> titresExistants = chargerTitresEvenements();
            Calendar cal = Calendar.getInstance();

            for (Object[] demo : EVENEMENTS_DEMO) {
                if (total >= NOMBRE_MINIMUM_EVENEMENTS) return;
                String titre = (String) demo[0];
                if (titresExistants.contains(titre.toLowerCase())) continue;

                Integer typeId = types.get(((String) demo[2]).toLowerCase());
                if (typeId == null) continue;

                cal.add(Calendar.DAY_OF_MONTH, 3);
                Date dateDebut = cal.getTime();
                cal.add(Calendar.HOUR_OF_DAY, 4);
                Date dateFin = cal.getTime();

                String sql = "INSERT INTO evenement (titre, description, type_id, date_debut, date_fin, lieu, nb_participants_max, statut, image, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";
                try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
                    ps.setString(1, titre);
                    ps.setString(2, (String) demo[1]);
                    ps.setInt(3, typeId);
                    ps.setTimestamp(4, new Timestamp(dateDebut.getTime()));
                    ps.setTimestamp(5, new Timestamp(dateFin.getTime()));
                    ps.setString(6, (String) demo[3]);
                    ps.setInt(7, (Integer) demo[4]);
                    ps.setString(8, (String) demo[5]);
                    ps.setString(9, (String) demo[6]);
                    ps.executeUpdate();
                }
                titresExistants.add(titre.toLowerCase());
                total++;
            }
        } catch (Exception e) {
            System.out.println("Erreur initialisation evenements demo: " + e.getMessage());
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
        try (Statement st = getCnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String titre = rs.getString("titre");
                if (titre != null) titres.add(titre.toLowerCase());
            }
        }
        return titres;
    }

    private int compterEvenements() throws SQLException {
        String sql = "SELECT COUNT(*) FROM evenement";
        try (Statement st = getCnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Evenement mapEvenement(ResultSet rs) throws SQLException {
        Evenement e = new Evenement();
        e.setId(rs.getInt("id"));
        e.setTitre(rs.getString("titre"));
        e.setDescription(rs.getString("description"));
        e.setTypeId(rs.getInt("type_id"));
        Timestamp tsDebut = rs.getTimestamp("date_debut");
        if (tsDebut != null) e.setDateDebut(new Date(tsDebut.getTime()));
        Timestamp tsFin = rs.getTimestamp("date_fin");
        if (tsFin != null) e.setDateFin(new Date(tsFin.getTime()));
        e.setLieu(rs.getString("lieu"));
        e.setNbParticipantsMax(rs.getInt("nb_participants_max"));
        e.setStatut(rs.getString("statut"));
        e.setImage(rs.getString("image"));
        e.setUserId(rs.getInt("user_id"));
        return e;
    }
}
