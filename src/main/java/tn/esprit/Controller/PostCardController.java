package tn.esprit.Controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import tn.esprit.api.TextFixClient;
import tn.esprit.api.ToxicityClient;
import tn.esprit.utils.SessionManager;
import tn.esprit.api.model.TextFixResult;
import tn.esprit.api.model.ToxicityResult;
import tn.esprit.entities.Comment;
import tn.esprit.entities.Post;
import tn.esprit.services.ServiceComment;
import tn.esprit.services.ServiceImagePost;
import tn.esprit.services.ServiceLike;
import tn.esprit.services.ServiceShare;
import tn.esprit.services.UserService;

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
    private Label trendingBadge;

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
    private TextFixClient textFixClient = new TextFixClient();
    private ToxicityClient toxicityClient = new ToxicityClient();
    private UserService userService = new UserService();
    private boolean commentsExpanded = false;

    private static final String DEFAULT_AVATAR = "uploads/profiles/default.png";

    private Consumer<Post> onEditCallback;
    private Runnable onDeleteCallback;
    private Runnable onShareCallback;

    private List<String> currentImages = new ArrayList<>();
    private int currentImageIndex = 0;
    private int postRank = Integer.MAX_VALUE;

    private String getProfilePhoto(int userId) {
        return userService.getProfilePhoto(userId);
    }

    private String getUsername(int userId) {
        return userService.getUsername(userId);
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

            if (trendingBadge != null && postRank <= 10) {
                trendingBadge.setVisible(true);
                trendingBadge.setManaged(true);
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

    public void setPostRank(int rank) {
        this.postRank = rank;
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

        serviceLike.toggleLike(post.getId(), SessionManager.getCurrentUser().getId());
        updateLikeCount();
        updateLikeButtonStyle();
    }

    @FXML
    private void handleShare() {
        if (post == null) return;

        if (onShareCallback != null) {
            onShareCallback.run();
        } else {
            serviceShare.addShare(new tn.esprit.entities.Share(post.getId(), SessionManager.getCurrentUser().getId(), "Shared post"));
            updateShareCount();
        }
    }

    private void loadUserAvatar() {
        if (userAvatarImage == null || post == null) return;

        String photoPath = getProfilePhoto(post.getUserId());
        double size = 40;

        if (loadAvatarImage(photoPath, userAvatarImage, size)) {
            return;
        }

        if (!DEFAULT_AVATAR.equals(photoPath)) {
            if (loadAvatarImage(DEFAULT_AVATAR, userAvatarImage, size)) {
                return;
            }
        }

        System.out.println("No avatar found for user " + post.getUserId());
    }

    private void loadCommentAvatar() {
        if (commentAvatarImage == null) return;

        String photoPath = getProfilePhoto(SessionManager.getCurrentUser().getId());
        double size = 28;

        if (loadAvatarImage(photoPath, commentAvatarImage, size)) {
            return;
        }

        if (!DEFAULT_AVATAR.equals(photoPath)) {
            if (loadAvatarImage(DEFAULT_AVATAR, commentAvatarImage, size)) {
                return;
            }
        }

        System.out.println("No avatar found for comment user");
    }

    private boolean loadAvatarImage(String path, ImageView imageView, double size) {
        try {
            File imgFile = new File(path).getAbsoluteFile();
            if (!imgFile.exists()) {
                return false;
            }
            Image image = new Image(imgFile.toURI().toString());
            if (image.isError()) {
                return false;
            }
            double scale = Math.max(size / image.getWidth(), size / image.getHeight());
            double fitW = image.getWidth() * scale;
            double fitH = image.getHeight() * scale;
            imageView.setFitWidth(fitW);
            imageView.setFitHeight(fitH);
            imageView.setImage(image);
            imageView.setPreserveRatio(true);
            imageView.setClip(new Circle(fitW / 2, fitH / 2, size / 2));
            return true;
        } catch (Exception e) {
            return false;
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

        boolean hasLiked = serviceLike.hasUserLiked(post.getId(), SessionManager.getCurrentUser().getId());
        if (hasLiked) {
            likeButton.getStyleClass().clear();
            likeButton.getStyleClass().add("liked-button");
        } else {
            likeButton.getStyleClass().clear();
            likeButton.getStyleClass().add("action-button");
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
            emptyLabel.getStyleClass().add("empty-comments-label");
            commentsList.getChildren().add(emptyLabel);
        } else {
            for (Comment c : comments) {
                commentsList.getChildren().add(createCommentRow(c));
            }
        }
    }

    private HBox createCommentRow(Comment comment) {
        HBox row = new HBox(10);
        row.getStyleClass().add("comment-row");

        StackPane avatarPane = new StackPane();
        Circle bgCircle = new Circle(12);
        bgCircle.setFill(javafx.scene.paint.Color.web("#2c3a4c"));
        bgCircle.setStroke(javafx.scene.paint.Color.web("#00eefc"));
        bgCircle.setStrokeWidth(1.0);

        ImageView avatarImg = new ImageView();
        avatarImg.setFitWidth(24);
        avatarImg.setFitHeight(24);
        avatarImg.setPreserveRatio(false);

        String photoPath = getProfilePhoto(comment.getUserId());
        File imgFile = new File(photoPath).getAbsoluteFile();
        if (imgFile.exists()) {
            Image image = new Image(imgFile.toURI().toString());
            double size = 24;
            double scale = Math.max(size / image.getWidth(), size / image.getHeight());
            double fitW = image.getWidth() * scale;
            double fitH = image.getHeight() * scale;
            avatarImg.setFitWidth(fitW);
            avatarImg.setFitHeight(fitH);
            avatarImg.setImage(image);
            avatarImg.setPreserveRatio(true);
            avatarImg.setClip(new Circle(fitW / 2, fitH / 2, size / 2));
        }

        avatarPane.getChildren().addAll(bgCircle, avatarImg);

        VBox textBlock = new VBox(2);
        HBox.setHgrow(textBlock, javafx.scene.layout.Priority.ALWAYS);

        Label authorLabel = new Label(getUsername(comment.getUserId()));
        authorLabel.getStyleClass().add("comment-author");

        Label textLabel = new Label(comment.getCommentText());
        textLabel.setWrapText(true);
        textLabel.getStyleClass().add("comment-text");

        String timeAgo = formatTimeAgo(comment.getCreatedAt().toInstant());
        Label timeLabel = new Label(timeAgo);
        timeLabel.getStyleClass().add("comment-time");

        textBlock.getChildren().addAll(authorLabel, textLabel, timeLabel);

        Button deleteBtn = new Button("x");
        deleteBtn.getStyleClass().add("comment-delete-btn");
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

        String textToCheck = text.trim();
        if (toxicityClient.isConfigured()) {
            new Thread(() -> {
                ToxicityResult result = toxicityClient.check(textToCheck);
                if (result != null && result.isToxic()) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING,
                            "⚠ Content Flagged\n\nYour comment was flagged as " + result.getLabel()
                            + " (" + String.format("%.0f", result.getScore() * 100) + "% confidence).\nPlease revise.",
                            ButtonType.OK);
                        alert.show();
                    });
                    return;
                }
                Platform.runLater(() -> addComment(textToCheck));
            }).start();
        } else {
            addComment(textToCheck);
        }
    }

    private void addComment(String text) {
        Comment newComment = new Comment(post.getId(), SessionManager.getCurrentUser().getId(), text);
        serviceComment.add(newComment);
        commentInput.clear();
        loadComments();
        updateCommentCount();
    }

    @FXML
    private void handleFixComment() {
        String text = commentInput.getText();
        if (text == null || text.trim().isEmpty()) return;

        new Thread(() -> {
            TextFixResult result = textFixClient.fix(text);
            Platform.runLater(() -> {
                if (result.isSuccess()) {
                    commentInput.setText(result.getFixedText());
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING, result.getError(), ButtonType.OK);
                    alert.show();
                }
            });
        }).start();
    }

    private void handleDeleteComment(Comment comment) {
        serviceComment.delete(comment.getId());
        loadComments();
        updateCommentCount();
    }

    private void updateCommentCount() {
        if (post == null) return;
        int count = serviceComment.getCommentCount(post.getId());
        commentsButton.setText(count > 0 ? "\uD83D\uDCAC " + count + " Comment" + (count > 1 ? "s" : "") : "\uD83D\uDCAC Comments");
    }
}
