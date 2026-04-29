package org.esprit.services;

import org.esprit.models.Annonce;
import org.esprit.utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnnonceService {
    private Connection cnx = MyDatabase.getInstance().getConnection();

    // CREATE
    public void ajouter(Annonce a) throws SQLException {
        String sql = "INSERT INTO annonce (titre, description, jeu, salaire, date_publication, statut, id_categorie) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, a.getTitre());
        ps.setString(2, a.getDescription());
        ps.setString(3, a.getJeu());
        ps.setDouble(4, a.getSalaire());
        ps.setDate(5, new java.sql.Date(System.currentTimeMillis()));
        ps.setString(6, a.getStatut());
        ps.setInt(7, a.getIdCategorie());
        ps.executeUpdate();
        System.out.println("✅ Annonce ajoutée !");
    }

    // READ ALL (avec JOIN pour récupérer le nom de la catégorie)
    public List<Annonce> getAll() throws SQLException {
        List<Annonce> list = new ArrayList<>();
        String sql = "SELECT a.*, c.nom AS nom_categorie " +
                "FROM annonce a LEFT JOIN categorie c ON a.id_categorie = c.id";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
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
            list.add(a);
        }
        return list;
    }

    // READ BY ID
    public Annonce getById(int id) throws SQLException {
        String sql = "SELECT a.*, c.nom AS nom_categorie " +
                "FROM annonce a LEFT JOIN categorie c ON a.id_categorie = c.id " +
                "WHERE a.id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
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
            return a;
        }
        return null;
    }

    // UPDATE
    public void modifier(Annonce a) throws SQLException {
        String sql = "UPDATE annonce SET titre=?, description=?, jeu=?, salaire=?, statut=?, id_categorie=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, a.getTitre());
        ps.setString(2, a.getDescription());
        ps.setString(3, a.getJeu());
        ps.setDouble(4, a.getSalaire());
        ps.setString(5, a.getStatut());
        ps.setInt(6, a.getIdCategorie());
        ps.setInt(7, a.getId());
        ps.executeUpdate();
        System.out.println("✅ Annonce modifiée !");
    }

    // DELETE
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM annonce WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("✅ Annonce supprimée !");
    }

    // SEARCH
    public List<Annonce> rechercher(String keyword) throws SQLException {
        List<Annonce> list = new ArrayList<>();
        String sql = "SELECT a.*, c.nom AS nom_categorie " +
                "FROM annonce a LEFT JOIN categorie c ON a.id_categorie = c.id " +
                "WHERE a.titre LIKE ? OR a.jeu LIKE ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, "%" + keyword + "%");
        ps.setString(2, "%" + keyword + "%");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Annonce a = new Annonce();
            a.setId(rs.getInt("id"));
            a.setTitre(rs.getString("titre"));
            a.setJeu(rs.getString("jeu"));
            a.setStatut(rs.getString("statut"));
            a.setNomCategorie(rs.getString("nom_categorie"));
            list.add(a);
        }
        return list;
    }
}