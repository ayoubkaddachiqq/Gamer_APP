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
        Scene scene = new Scene(root, 1300, 760);

        String css = getClass().getResource("/styles/style.css").toExternalForm();
        scene.getStylesheets().add(css);

        primaryStage.setTitle("TeamHub - Gestion des Annonces");
        primaryStage.setMinWidth(1300);
        primaryStage.setMinHeight(760);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
