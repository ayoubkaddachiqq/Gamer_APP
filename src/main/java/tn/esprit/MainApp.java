package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Note: The path starts from the 'resources' folder
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainInterface.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1100, 700);

        primaryStage.setTitle("Team Hub - E-Sport Recruitment");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}