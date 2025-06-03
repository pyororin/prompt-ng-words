package com.example.promptngapi.dto;

import jakarta.validation.constraints.NotBlank;

public class PromptRequest {

    @NotBlank(message = "Text cannot be blank")
    private String text;

    // Default constructor
    public PromptRequest() {
    }

    // Constructor with fields
    public PromptRequest(String text) {
        this.text = text;
    }

    // Getter
    public String getText() {
        return text;
    }

    // Setter
    public void setText(String text) {
        this.text = text;
    }
}
