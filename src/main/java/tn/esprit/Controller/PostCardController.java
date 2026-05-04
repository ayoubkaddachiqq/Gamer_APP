package tn.esprit.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Comment;
import tn.esprit.entities.Post;
import tn.esprit.services.ServiceComment;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

public class PostCardController {

    @FXML
    private Label userNameLabel;

    @FXML
    private Label gameTagLabel;

    @FXML
    private Label contentLabel;

    @FXML
    private Label postDateLabel;

    @FXML
    private Button commentsButton;

    @FXML
    private VBox commentsSection;

    @FXML
    private VBox commentsList;

    @FXML
    private TextField commentInput;

    @FXML
    private HBox editActions;

    @FXML
    private Button likeButton;

    @FXML
    private Button shareButton;

    private Post post;
    private ServiceComment serviceComment = new ServiceComment();
    private boolean commentsExpanded = false;

    private Consumer<Post> onEditCallback;
    private Runnable onDeleteCallback;

    public void setData(Post post) {
        this.post = post;
        if (post != null) {
            contentLabel.setText(post.getContent());

            if (gameTagLabel != null && post.getGameTag() != null) {
                gameTagLabel.setText(post.getGameTag().toUpperCase());
            }

            if (userNameLabel != null) {
                userNameLabel.setText("Player " + post.getUserId());
            }

            if (postDateLabel != null && post.getCreatedAt() != null) {
                postDateLabel.setText(formatTimeAgo(post.getCreatedAt().toInstant()));
            }

            updateCommentCount();
        }
    }

    public void setEditable(boolean editable) {
        if (editActions != null) {
            editActions.setVisible(editable);
            editActions.setManaged(editable);
        }
        if (commentsButton != null) {
            commentsButton.setVisible(!editable);
            commentsButton.setManaged(!editable);
        }
        if (commentsSection != null) {
            commentsSection.setVisible(!editable);
            commentsSection.setManaged(!editable);
        }
        if (likeButton != null) {
            likeButton.setVisible(!editable);
            likeButton.setManaged(!editable);
        }
        if (shareButton != null) {
            shareButton.setVisible(!editable);
            shareButton.setManaged(!editable);
        }
    }

    public void setOnEdit(Consumer<Post> callback) {
        this.onEditCallback = callback;
    }

    public void setOnDelete(Runnable callback) {
        this.onDeleteCallback = callback;
    }

    @FXML
    private void toggleComments() {
        commentsExpanded = !commentsExpanded;
        commentsSection.setVisible(commentsExpanded);
        commentsSection.setManaged(commentsExpanded);

        if (commentsExpanded && post != null) {
            loadComments();
        }
    }

    @FXML
    private void handleEdit() {
        if (onEditCallback != null && post != null) {
            onEditCallback.accept(post);
        }
    }

    @FXML
    private void handleDelete() {
        if (onDeleteCallback != null && post != null) {
            onDeleteCallback.run();
        }
    }

    private void loadComments() {
        commentsList.getChildren().clear();
        if (post == null) return;

        List<Comment> comments = serviceComment.getCommentsByPost(post.getId());
        if (comments.isEmpty()) {
            Label emptyLabel = new Label("No comments yet. Be the first!");
            emptyLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-padding: 10 0;");
            commentsList.getChildren().add(emptyLabel);
        } else {
            for (Comment c : comments) {
                commentsList.getChildren().add(createCommentRow(c));
            }
        }
    }

    private HBox createCommentRow(Comment comment) {
        HBox row = new HBox(10);
        row.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-padding: 10;");

        VBox textBlock = new VBox(2);
        HBox.setHgrow(textBlock, javafx.scene.layout.Priority.ALWAYS);

        Label authorLabel = new Label("Player " + comment.getUserId());
        authorLabel.setStyle("-fx-text-fill: #00eefc; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label textLabel = new Label(comment.getCommentText());
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

        String timeAgo = formatTimeAgo(comment.getCreatedAt().toInstant());
        Label timeLabel = new Label(timeAgo);
        timeLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 10px;");

        textBlock.getChildren().addAll(authorLabel, textLabel, timeLabel);

        Button deleteBtn = new Button("✕");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff4b4b; -fx-font-size: 10px; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeleteComment(comment));

        row.getChildren().addAll(textBlock, deleteBtn);
        return row;
    }

    private String formatTimeAgo(Instant instant) {
        Duration duration = Duration.between(instant, Instant.now());
        long seconds = duration.getSeconds();

        if (seconds < 60) return "Just now";
        if (seconds < 3600) return (seconds / 60) + " min ago";
        if (seconds < 86400) return (seconds / 3600) + "h ago";
        return (seconds / 86400) + "d ago";
    }

    @FXML
    private void handleAddComment() {
        String text = commentInput.getText();
        if (text == null || text.trim().isEmpty() || post == null) return;

        Comment newComment = new Comment(post.getId(), 1, text.trim());
        serviceComment.add(newComment);
        commentInput.clear();
        loadComments();
        updateCommentCount();
    }

    private void handleDeleteComment(Comment comment) {
        serviceComment.delete(comment.getId());
        loadComments();
        updateCommentCount();
    }

    private void updateCommentCount() {
        if (post == null) return;
        List<Comment> comments = serviceComment.getCommentsByPost(post.getId());
        int count = comments.size();
        commentsButton.setText(count > 0 ? "💬 " + count + " Comment" + (count > 1 ? "s" : "") : "💬 Comments");
    }
}
