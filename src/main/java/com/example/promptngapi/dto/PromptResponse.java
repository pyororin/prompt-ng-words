package com.example.promptngapi.dto;

public class PromptResponse {

    private boolean result;

    // Default constructor
    public PromptResponse() {
    }

    // Constructor with fields
    public PromptResponse(boolean result) {
        this.result = result;
    }

    // Getter
    public boolean isResult() {
        return result;
    }

    // Setter
    public void setResult(boolean result) {
        this.result = result;
    }
}
