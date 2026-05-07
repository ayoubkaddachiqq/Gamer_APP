package tn.esprit.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource("/tn/esprit/demo/view/login.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load(), 1000, 700);
        stage.setTitle("TeamHub – Login");
        stage.setScene(scene);
        stage.show();
    }
}

