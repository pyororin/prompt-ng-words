package com.example.promptngapi.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * プロンプト判定APIのリクエストボディを表します。
 */
public class PromptRequest {

    /**
     * 判定対象のテキストコンテンツです。
     * 空白であってはなりません。
     */
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
