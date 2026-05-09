package tn.esprit.api.model;

public class TextFixResult {
    private String fixedText;
    private boolean success;
    private String error;

    public TextFixResult() {}

    public TextFixResult(String fixedText) {
        this.fixedText = fixedText;
        this.success = true;
    }

    public TextFixResult(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    public String getFixedText() { return fixedText; }
    public void setFixedText(String fixedText) { this.fixedText = fixedText; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
