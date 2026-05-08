package org.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.esprit.security.Session;
import java.io.IOException;

public class MainFx extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        String startPage = Session.isAdmin() ? "/GestionEvenement.fxml" : "/GestionInscription.fxml";
        FXMLLoader loader = new FXMLLoader(getClass().getResource(startPage));
        try {
            Parent root = loader.load();
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("TeamHub");
            primaryStage.setWidth(1200);
            primaryStage.setHeight(800);
            primaryStage.show();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
