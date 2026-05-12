package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDB {


    private static MyDB instance;


    private Connection connection;


    private final String url = "jdbc:mysql://localhost:3306/gamer_app";
    private final String user = "ayoub";
    private final String password = "kaddachi123";

    private MyDB() {
        try {

            this.connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connexion à MySQL réussie !");

        } catch (SQLException e) {
            System.err.println("Erreur de connexion à la base de données : " + e.getMessage());
        }
    }


    public static synchronized MyDB getInstance() {
        if (instance == null) {
            instance = new MyDB();
        } else {
            try {
                if (instance.getConnection().isClosed()) {
                    instance = new MyDB();
                }
            } catch (SQLException e) {
                instance = new MyDB();
            }
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
