module tn.esprit.gamer {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires stripe.java;

    exports tn.esprit;
    opens tn.esprit.controllers to javafx.fxml, javafx.base;
    opens tn.esprit.model to javafx.base;
}
