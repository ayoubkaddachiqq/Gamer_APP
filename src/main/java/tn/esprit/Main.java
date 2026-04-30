package tn.esprit;

import java.sql.Connection;

import tn.esprit.utils.MyDB;
import tn.esprit.entities.Post;
import tn.esprit.entities.Comment;
import tn.esprit.services.ServicePost;
import tn.esprit.services.ServiceComment;
import java.util.List;
//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        ServiceComment sc = new ServiceComment();

        System.out.println("========== TEAM HUB: COMMENT FLOW TEST ==========");

        // Test 1: Retrieve comments for Post #1 (Expected: 3)
        int testPostId = 2;
        System.out.println("\n[Action] Fetching comments for Post ID: " + testPostId);

        List<Comment> post1Comments = sc.getCommentsByPost(testPostId);

        if (post1Comments.isEmpty()) {
            System.out.println("Result: No comments found or error in query.");
        } else {
            System.out.println("Result: Found " + post1Comments.size() + " comments.");
            for (Comment c : post1Comments) {
                System.out.println("  - [" + c.getCreatedAt() + "] User " + c.getUserId() + ": " + c.getCommentText());
            }
        }

        // Test 2: Add a new comment (Simulating User Interaction)
        System.out.println("\n[Action] Adding a new comment to Post #3...");
        Comment newComm = new Comment(3, 1, "Adding a 2nd comment to Post 3 for the flow test!");
        sc.add(newComm);

        // Test 3: Verify the update
        System.out.println("Result: Post #3 now has " + sc.getCommentsByPost(3).size() + " comments.");

        System.out.println("\n================ TEST COMPLETE ================");
    }
}