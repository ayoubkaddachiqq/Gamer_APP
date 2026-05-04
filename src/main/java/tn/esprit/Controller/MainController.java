package tn.esprit.Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.Post;
import tn.esprit.services.ServicePost; // Ensure this is imported

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainController {

    @FXML
    private VBox feedContainer;

    @FXML
    private TextArea postInput; // Linked to your FXML TextArea

    private ServicePost servicePost = new ServicePost();


    @FXML private ComboBox<String> gameTagSelector;

    @FXML
    public void initialize() {
        System.out.println("Initializing controller...");
        gameTagSelector.getItems().addAll(
            "General", "VALORANT", "League of Legends", "CS2", "Fortnite",
            "Apex Legends", "Overwatch 2", "Dota 2", "Rocket League",
            "EA FC 25", "Call of Duty", "Minecraft", "GTA V", "Rainbow Six Siege"
        );
        gameTagSelector.setValue("General");

        try {
            List<Post> posts = servicePost.getAll();
            System.out.println("Fetched " + posts.size() + " posts from DB.");
            for (Post p : posts) {
                addPostToFeed(p);
            }
        } catch (Exception e) {
            System.err.println("Database loading failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleCreatePost() {
        String content = postInput.getText();
        String selectedGame = gameTagSelector.getValue();

        if (content == null || content.trim().isEmpty()) return;

        Post newPost = new Post();
        newPost.setContent(content);
        newPost.setGameTag(selectedGame != null ? selectedGame : "General");
        newPost.setUserId(1);

        servicePost.add(newPost);
        addPostToFeed(newPost);
        postInput.clear();
    }

    @FXML
    private void handleMedia() {
        // Open a FileChooser to let the user select an image
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            System.out.println("Media selected: " + selectedFile.getAbsolutePath());
        }
    }

    private void addPostToFeed(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PostCard.fxml"));
            VBox card = loader.load();
            PostCardController cardController = loader.getController();
            cardController.setData(post);
            feedContainer.getChildren().add(0, card);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMyPosts() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MyPosts.fxml"));
            javafx.scene.Parent root = loader.load();
            MyPostsController controller = loader.getController();
            controller.init((Stage) feedContainer.getScene().getWindow());
            Stage stage = (Stage) feedContainer.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 700));
            stage.setTitle("My Posts - Team Hub");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleHome() {
        feedContainer.getChildren().clear();
        List<Post> posts = servicePost.getAll();
        for (Post p : posts) {
            addPostToFeed(p);
        }
    }
}