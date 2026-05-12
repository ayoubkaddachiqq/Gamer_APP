package tn.esprit.services;

import tn.esprit.entities.Annonce;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnnonceService {

    private static final Object[][] ANNONCES_PAR_DEFAUT = {
        {"Recherche Duelist Valorant", "Equipe semi-pro cherche un joueur actif pour scrims et tournois chaque weekend.", "Valorant", 3500.0, "OUVERTE", "Attaquant", "/images/annonces/valorant.png"},
        {"Support League of Legends", "Besoin d'un support serieux, disponible le soir, bon niveau macro et communication vocale.", "League of Legends", 2800.0, "OUVERTE", "Support", "/images/annonces/lol.png"},
        {"Coach Fortnite", "Structure e-sport recherche coach pour encadrer les joueurs et preparer les strategies.", "Fortnite", 4500.0, "EN_ATTENTE", "Coach", "/images/annonces/fortnite.png"},
        {"Defenseur Rocket League", "Equipe recrute un defenseur solide pour competition locale et entrainements reguliers.", "Rocket League", 2200.0, "OUVERTE", "Defenseur", "/images/annonces/rocket-league.png"},
        {"Strategiste CS2", "Nous cherchons un profil tactique pour analyser les matchs et preparer les calls.", "CS2", 3200.0, "OUVERTE", "Strategiste", "/images/annonces/cs2.png"},
        {"Tank Overwatch 2", "Roster cherche un tank principal avec experience en ranked et bonne communication.", "Overwatch 2", 3000.0, "FERMEE", "Tank", "/images/annonces/overwatch.png"}
    };

    public AnnonceService() {
        ensureImageColumn();
    }

    private Connection getCnx() {
        return MyDB.getInstance().getConnection();
    }

    public void valider(Annonce a) throws Exception {
        if (a.getTitre() == null || a.getTitre().trim().isEmpty())
            throw new Exception("Le titre est obligatoire");
        if (a.getTitre().length() < 5)
            throw new Exception("Le titre doit contenir au moins 5 caracteres");
        if (a.getTitre().length() > 200)
            throw new Exception("Le titre ne doit pas depasser 200 caracteres");
        if (a.getDescription() == null || a.getDescription().trim().isEmpty())
            throw new Exception("La description est obligatoire");
        if (a.getJeu() == null || a.getJeu().trim().isEmpty())
            throw new Exception("Le jeu est obligatoire");
        if (a.getSalaire() < 0)
            throw new Exception("Le salaire ne peut pas etre negatif");
        if (a.getSalaire() > 100000)
            throw new Exception("Le salaire semble invalide (max 100 000)");
        if (a.getIdCategorie() <= 0)
            throw new Exception("Veuillez choisir une categorie");
        List<String> statutsValides = List.of("OUVERTE", "FERMEE", "EN_ATTENTE");
        if (!statutsValides.contains(a.getStatut()))
            throw new Exception("Statut invalide. Valeurs acceptees : OUVERTE, FERMEE, EN_ATTENTE");
    }

    public void ajouter(Annonce a) throws Exception {
        valider(a);
        String sql = "INSERT INTO annonce (titre, description, jeu, salaire, date_publication, statut, id_categorie, user_id, image_path) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, a.getTitre());
            ps.setString(2, a.getDescription());
            ps.setString(3, a.getJeu());
            ps.setDouble(4, a.getSalaire());
            ps.setDate(5, new java.sql.Date(System.currentTimeMillis()));
            ps.setString(6, a.getStatut());
            ps.setInt(7, a.getIdCategorie());
            ps.setInt(8, a.getUserId());
            ps.setString(9, a.getImagePath());
            ps.executeUpdate();
        }
    }

    public void modifier(Annonce a) throws Exception {
        valider(a);
        String sql = "UPDATE annonce SET titre=?, description=?, jeu=?, salaire=?, statut=?, id_categorie=?, image_path=? WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, a.getTitre());
            ps.setString(2, a.getDescription());
            ps.setString(3, a.getJeu());
            ps.setDouble(4, a.getSalaire());
            ps.setString(5, a.getStatut());
            ps.setInt(6, a.getIdCategorie());
            ps.setString(7, a.getImagePath());
            ps.setInt(8, a.getId());
            ps.executeUpdate();
        }
    }

    public List<Annonce> getAll() throws SQLException {
        List<Annonce> list = new ArrayList<>();
        String sql = "SELECT a.*, c.nom AS nom_categorie FROM annonce a LEFT JOIN categorie c ON a.id_categorie = c.id ORDER BY a.date_publication DESC";
        try (Statement st = getCnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapAnnonce(rs));
            }
        }
        return list;
    }

    public Annonce getById(int id) throws SQLException {
        String sql = "SELECT a.*, c.nom AS nom_categorie FROM annonce a LEFT JOIN categorie c ON a.id_categorie = c.id WHERE a.id = ?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapAnnonce(rs);
            }
        }
        return null;
    }

    public List<Annonce> getByUserId(int userId) throws SQLException {
        List<Annonce> list = new ArrayList<>();
        String sql = "SELECT a.*, c.nom AS nom_categorie FROM annonce a LEFT JOIN categorie c ON a.id_categorie = c.id WHERE a.user_id = ? ORDER BY a.date_publication DESC";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAnnonce(rs));
            }
        }
        return list;
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM annonce WHERE id=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Annonce> rechercher(String keyword) throws SQLException {
        List<Annonce> list = new ArrayList<>();
        String sql = "SELECT a.*, c.nom AS nom_categorie FROM annonce a LEFT JOIN categorie c ON a.id_categorie = c.id WHERE a.titre LIKE ? OR a.jeu LIKE ?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAnnonce(rs));
            }
        }
        return list;
    }

    public void assurerAnnoncesParDefaut() throws Exception {
        CategorieService categorieService = new CategorieService();
        categorieService.assurerCategoriesParDefaut();

        for (Object[] annonce : ANNONCES_PAR_DEFAUT) {
            String titre = (String) annonce[0];
            if (titreExiste(titre)) {
                mettreAJourImageParDefaut(titre, (String) annonce[6]);
                continue;
            }
            Integer idCategorie = trouverIdCategorie((String) annonce[5]);
            if (idCategorie == null) continue;

            String sql = "INSERT INTO annonce (titre, description, jeu, salaire, date_publication, statut, id_categorie, user_id, image_path) VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?)";
            try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
                ps.setString(1, titre);
                ps.setString(2, (String) annonce[1]);
                ps.setString(3, (String) annonce[2]);
                ps.setDouble(4, (Double) annonce[3]);
                ps.setDate(5, new java.sql.Date(System.currentTimeMillis()));
                ps.setString(6, (String) annonce[4]);
                ps.setInt(7, idCategorie);
                ps.setString(8, (String) annonce[6]);
                ps.executeUpdate();
            }
        }
    }

    private boolean titreExiste(String titre) throws SQLException {
        String sql = "SELECT COUNT(*) FROM annonce WHERE titre = ?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, titre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void mettreAJourImageParDefaut(String titre, String imagePath) throws SQLException {
        String sql = "UPDATE annonce SET image_path = ? WHERE titre = ? AND (image_path IS NULL OR image_path = '')";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, imagePath);
            ps.setString(2, titre);
            ps.executeUpdate();
        }
    }

    private Integer trouverIdCategorie(String nomCategorie) throws SQLException {
        String sql = "SELECT id FROM categorie WHERE nom = ?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setString(1, nomCategorie);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : null;
            }
        }
    }

    private Annonce mapAnnonce(ResultSet rs) throws SQLException {
        Annonce a = new Annonce();
        a.setId(rs.getInt("id"));
        a.setTitre(rs.getString("titre"));
        a.setDescription(rs.getString("description"));
        a.setJeu(rs.getString("jeu"));
        a.setSalaire(rs.getDouble("salaire"));
        a.setDatePublication(rs.getDate("date_publication"));
        a.setStatut(rs.getString("statut"));
        a.setIdCategorie(rs.getInt("id_categorie"));
        a.setNomCategorie(rs.getString("nom_categorie"));
        a.setUserId(rs.getInt("user_id"));
        a.setImagePath(rs.getString("image_path"));
        return a;
    }

    private void ensureImageColumn() {
        try {
            DatabaseMetaData metaData = getCnx().getMetaData();
            try (ResultSet rs = metaData.getColumns(getCnx().getCatalog(), null, "annonce", "image_path")) {
                if (rs.next()) return;
            }
            try (Statement st = getCnx().createStatement()) {
                st.executeUpdate("ALTER TABLE annonce ADD COLUMN image_path VARCHAR(500)");
            }
        } catch (SQLException e) {
            System.out.println("Cannot verify image_path column: " + e.getMessage());
        }
    }
}
