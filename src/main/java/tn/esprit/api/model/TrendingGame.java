package tn.esprit.api.model;

public class TrendingGame {
    private String name;
    private double rating;
    private String imageUrl;

    public TrendingGame() {}

    public TrendingGame(String name, double rating, String imageUrl) {
        this.name = name;
        this.rating = rating;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
