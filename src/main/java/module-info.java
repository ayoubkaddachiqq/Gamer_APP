module tn.esprit.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.net.http;
    requires org.json;
    requires mysql.connector.j;
    requires com.zaxxer.hikari;
    requires jbcrypt;
    requires jakarta.mail;

    // Original package
    opens tn.esprit.demo to javafx.fxml;
    exports tn.esprit.demo;

    // User Management controllers (com.teamhub.controller)
    opens tn.esprit.demo.controller to javafx.fxml;
    exports tn.esprit.demo.controller;
}
