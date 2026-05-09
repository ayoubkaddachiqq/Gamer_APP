package tn.esprit.api.model;

public class ToxicityResult {
    private String label;
    private double score;
    private boolean toxic;

    public ToxicityResult() {}

    public ToxicityResult(String label, double score, boolean toxic) {
        this.label = label;
        this.score = score;
        this.toxic = toxic;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public boolean isToxic() { return toxic; }
    public void setToxic(boolean toxic) { this.toxic = toxic; }
}
