package tn.esprit.Controller;
import javafx.fxml.FXMLLoader;
import tn.esprit.entities.Post;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class MainController {

    // This ID must match the fx:id="feedContainer" in your MainInterface.fxml
    @FXML
    private VBox feedContainer;

    /**
     * This method runs as soon as the FXML is loaded.
     */
    @FXML
    public void initialize() {
        System.out.println("Interface loaded successfully!");
        loadTemporaryData();


    }

    // Inside MainController.java
    private void loadTemporaryData() {
        for (int i = 1; i <= 3; i++) {
            Post dummy = new Post();
            dummy.setContent("Looking for a duo mate to climb the ranks! Message me if interested.");
            dummy.setGameTag("VALORANT");

            addPostToFeed(dummy);
        }
    }
    private void addPostToFeed(Post post) {
        try {
            // 1. Load the small PostCard design
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PostCard.fxml"));
            VBox card = loader.load();

            // 2. Get the controller of that specific card
            PostCardController cardController = loader.getController();

            // 3. Send the Post data to that card's controller
            cardController.setData(post);

            // 4. Add the card to the main window's feed
            feedContainer.getChildren().add(card);

        } catch (IOException e) {
            System.err.println("Error: Could not find or load PostCard.fxml");
            e.printStackTrace();
        }
    }
}