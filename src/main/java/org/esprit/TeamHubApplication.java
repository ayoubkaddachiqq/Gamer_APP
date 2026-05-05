package org.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TeamHubApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/GestionAnnonce.fxml"));
        Scene scene = new Scene(root, 1000, 700);

        String css = getClass().getResource("/styles/style.css").toExternalForm();
        scene.getStylesheets().add(css);

        primaryStage.setTitle("TeamHub - Gestion des Annonces");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
