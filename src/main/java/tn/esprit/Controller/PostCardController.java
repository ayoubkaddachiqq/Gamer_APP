package tn.esprit.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import tn.esprit.entities.Comment;
import tn.esprit.entities.Post;
import tn.esprit.services.ServiceComment;
import tn.esprit.services.ServiceImagePost;
import tn.esprit.services.ServiceLike;
import tn.esprit.services.ServiceShare;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
    private ImageView userAvatarImage;

    @FXML
    private ImageView commentAvatarImage;

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
    private Label likeCountLabel;

    @FXML
    private Button shareButton;

    @FXML
    private Label shareCountLabel;

    @FXML
    private VBox imageContainer;

    @FXML
    private ImageView postImageView;

    @FXML
    private Button prevImageBtn;

    @FXML
    private Button nextImageBtn;

    @FXML
    private Label imageCounter;

    private Post post;
    private ServiceComment serviceComment = new ServiceComment();
    private ServiceLike serviceLike = new ServiceLike();
    private ServiceShare serviceShare = new ServiceShare();
    private ServiceImagePost serviceImagePost = new ServiceImagePost();
    private boolean commentsExpanded = false;

    private static final int CURRENT_USER_ID = 1;
    private static final String DEFAULT_AVATAR = "uploads/profiles/default.png";

    private Consumer<Post> onEditCallback;
    private Runnable onDeleteCallback;
    private Runnable onShareCallback;

    private List<String> currentImages = new ArrayList<>();
    private int currentImageIndex = 0;

    private String getProfilePhoto(int userId) {
        try (java.sql.PreparedStatement ps = tn.esprit.utils.MyDB.getInstance().getConnection().prepareStatement("SELECT profile_photo FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String path = rs.getString("profile_photo");
                if (path != null && !path.isEmpty()) return path;
            }
        } catch (Exception e) { /* ignore */ }
        return DEFAULT_AVATAR;
    }

    public void setData(Post post) {
        this.post = post;
        if (post != null) {
            contentLabel.setText(post.getContent());

            if (gameTagLabel != null && post.getGameTag() != null) {
                gameTagLabel.setText(post.getGameTag().toUpperCase());
            }

            if (userNameLabel != null) {
                String displayName = post.getUsername() != null && !post.getUsername().isEmpty() 
                    ? post.getUsername() 
                    : "Player " + post.getUserId();
                userNameLabel.setText(displayName);
            }

            if (postDateLabel != null && post.getCreatedAt() != null) {
                postDateLabel.setText(formatTimeAgo(post.getCreatedAt().toInstant()));
            }

            loadUserAvatar();
            loadCommentAvatar();
            loadImages();
            updateCommentCount();
            updateLikeCount();
            updateShareCount();
            updateLikeButtonStyle();
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
        if (likeCountLabel != null) {
            likeCountLabel.setVisible(!editable);
            likeCountLabel.setManaged(!editable);
        }
        if (shareButton != null) {
            shareButton.setVisible(!editable);
            shareButton.setManaged(!editable);
        }
        if (shareCountLabel != null) {
            shareCountLabel.setVisible(!editable);
            shareCountLabel.setManaged(!editable);
        }
        if (imageContainer != null) {
            imageContainer.setVisible(!editable);
            imageContainer.setManaged(!editable);
        }
    }

    public void setOnEdit(Consumer<Post> callback) {
        this.onEditCallback = callback;
    }

    public void setOnDelete(Runnable callback) {
        this.onDeleteCallback = callback;
    }

    public void setOnShare(Runnable callback) {
        this.onShareCallback = callback;
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

    @FXML
    private void handleLike() {
        if (post == null) return;

        serviceLike.toggleLike(post.getId(), CURRENT_USER_ID);
        updateLikeCount();
        updateLikeButtonStyle();
    }

    @FXML
    private void handleShare() {
        if (post == null) return;

        if (onShareCallback != null) {
            onShareCallback.run();
        } else {
            serviceShare.addShare(new tn.esprit.entities.Share(post.getId(), CURRENT_USER_ID, "Shared post"));
            updateShareCount();
        }
    }

    private void loadUserAvatar() {
        if (userAvatarImage == null || post == null) return;

        String photoPath = getProfilePhoto(post.getUserId());
        File imgFile = new File(photoPath).getAbsoluteFile();
        if (imgFile.exists()) {
            Image image = new Image(imgFile.toURI().toString());
            double size = 40;
            double scale = Math.max(size / image.getWidth(), size / image.getHeight());
            userAvatarImage.setFitWidth(image.getWidth() * scale);
            userAvatarImage.setFitHeight(image.getHeight() * scale);
            userAvatarImage.setImage(image);
            userAvatarImage.setPreserveRatio(true);
            userAvatarImage.setClip(new Circle(size / 2));
        } else {
            System.out.println("Avatar not found at: " + imgFile.getAbsolutePath());
        }
    }

    private void loadCommentAvatar() {
        if (commentAvatarImage == null) return;

        String photoPath = getProfilePhoto(CURRENT_USER_ID);
        File imgFile = new File(photoPath).getAbsoluteFile();
        if (imgFile.exists()) {
            Image image = new Image(imgFile.toURI().toString());
            double size = 28;
            double scale = Math.max(size / image.getWidth(), size / image.getHeight());
            commentAvatarImage.setFitWidth(image.getWidth() * scale);
            commentAvatarImage.setFitHeight(image.getHeight() * scale);
            commentAvatarImage.setImage(image);
            commentAvatarImage.setPreserveRatio(true);
            commentAvatarImage.setClip(new Circle(size / 2));
        } else {
            System.out.println("Comment avatar not found at: " + imgFile.getAbsolutePath());
        }
    }

    private void loadImages() {
        currentImages.clear();
        currentImageIndex = 0;

        if (post == null) return;

        List<tn.esprit.entities.ImagePost> dbImages = serviceImagePost.getImagesByPost(post.getId());
        for (tn.esprit.entities.ImagePost img : dbImages) {
            currentImages.add(img.getImagePath());
        }

        if (currentImages.isEmpty()) {
            imageContainer.setVisible(false);
            imageContainer.setManaged(false);
            return;
        }

        imageContainer.setVisible(true);
        imageContainer.setManaged(true);

        if (currentImages.size() > 1) {
            prevImageBtn.setVisible(true);
            prevImageBtn.setManaged(true);
            nextImageBtn.setVisible(true);
            nextImageBtn.setManaged(true);
            imageCounter.setVisible(true);
            imageCounter.setManaged(true);
        }

        displayCurrentImage();
    }

    private void displayCurrentImage() {
        if (currentImages.isEmpty() || postImageView == null) return;

        String path = currentImages.get(currentImageIndex);
        File imgFile = new File(path);
        if (imgFile.exists()) {
            Image image = new Image(imgFile.toURI().toString(), 500, 300, true, true);
            postImageView.setImage(image);
        }

        if (imageCounter != null) {
            imageCounter.setText((currentImageIndex + 1) + " / " + currentImages.size());
        }

        if (prevImageBtn != null) {
            prevImageBtn.setVisible(currentImages.size() > 1);
        }
        if (nextImageBtn != null) {
            nextImageBtn.setVisible(currentImages.size() > 1);
        }
    }

    @FXML
    private void prevImage() {
        if (currentImages.isEmpty()) return;
        currentImageIndex = (currentImageIndex - 1 + currentImages.size()) % currentImages.size();
        displayCurrentImage();
    }

    @FXML
    private void nextImage() {
        if (currentImages.isEmpty()) return;
        currentImageIndex = (currentImageIndex + 1) % currentImages.size();
        displayCurrentImage();
    }

    private void updateLikeButtonStyle() {
        if (likeButton == null || post == null) return;

        boolean hasLiked = serviceLike.hasUserLiked(post.getId(), CURRENT_USER_ID);
        if (hasLiked) {
            likeButton.setStyle("-fx-background-color: rgba(189, 0, 255, 0.3); -fx-text-fill: #bd00ff; -fx-font-size: 16px; -fx-cursor: hand; -fx-background-radius: 5;");
        } else {
            likeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-cursor: hand;");
        }
    }

    private void updateLikeCount() {
        if (post == null || likeCountLabel == null) return;
        int count = serviceLike.getLikeCount(post.getId());
        likeCountLabel.setText(String.valueOf(count));
    }

    private void updateShareCount() {
        if (post == null || shareCountLabel == null) return;
        int count = serviceShare.getShareCount(post.getId());
        shareCountLabel.setText(String.valueOf(count));
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

        StackPane avatarPane = new StackPane();
        Circle bgCircle = new Circle(12);
        bgCircle.setFill(javafx.scene.paint.Color.web("#2c3a4c"));
        bgCircle.setStroke(javafx.scene.paint.Color.web("#00eefc"));
        bgCircle.setStrokeWidth(1.0);

        ImageView avatarImg = new ImageView();
        avatarImg.setFitWidth(24);
        avatarImg.setFitHeight(24);
        avatarImg.setPreserveRatio(false);
        Circle clip = new Circle(12);
        avatarImg.setClip(clip);

        String photoPath = getProfilePhoto(comment.getUserId());
        File imgFile = new File(photoPath).getAbsoluteFile();
        if (imgFile.exists()) {
            Image image = new Image(imgFile.toURI().toString());
            double size = 24;
            double scale = Math.max(size / image.getWidth(), size / image.getHeight());
            avatarImg.setFitWidth(image.getWidth() * scale);
            avatarImg.setFitHeight(image.getHeight() * scale);
            avatarImg.setImage(image);
            avatarImg.setPreserveRatio(true);
        }

        avatarPane.getChildren().addAll(bgCircle, avatarImg);

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

        Button deleteBtn = new Button("x");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ff4b4b; -fx-font-size: 10px; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeleteComment(comment));

        row.getChildren().addAll(avatarPane, textBlock, deleteBtn);
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

        Comment newComment = new Comment(post.getId(), CURRENT_USER_ID, text.trim());
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
