package tn.esprit.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import tn.esprit.entities.Post;

public class PostCardController {

    @FXML
    private Label userNameLabel; // This must match the fx:id exactly

    @FXML
    private Label gameTagLabel;

    @FXML
    private Label contentLabel;

    public void setData(Post post) {
        if (post != null) {
            contentLabel.setText(post.getContent());

            if (gameTagLabel != null && post.getGameTag() != null) {
                gameTagLabel.setText(post.getGameTag().toUpperCase());
            }

            if (userNameLabel != null) {
                // Since your Post entity has userId (int), we convert it to String
                userNameLabel.setText("Player " + post.getUserId());
            }
        }
    }
}