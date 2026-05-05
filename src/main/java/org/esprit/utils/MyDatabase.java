package org.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;

public class MyDatabase {
    private static MyDatabase instance;
    private Connection connection;

    private static final String URL = "jdbc:mysql://localhost:3306/TeamHub?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // vide pour XAMPP

    private MyDatabase() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connexion réussie !");
        } catch (Exception e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
    }

    public static MyDatabase getInstance() {
        if (instance == null) instance = new MyDatabase();
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}