package com.example.integrationtester;

public class TestResult {
    private String prompt;
    private String category;
    private String status; // "passed" or "failed"
    private String reason;

    public TestResult(String prompt, String category, String status, String reason) {
        this.prompt = prompt;
        this.category = category;
        this.status = status;
        this.reason = reason;
    }

    // Getters and Setters
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @Override
    public String toString() {
        return "TestResult{" +
                "prompt='" + prompt + '\'' +
                ", category='" + category + '\'' +
                ", status='" + status + '\'' +
                (reason != null && !reason.isEmpty() ? ", reason='" + reason + '\'' : "") +
                '}';
    }
}
