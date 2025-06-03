package com.example.promptngapi.dto;

/**
 * プロンプト判定APIのレスポンスボディを表します。
 */
public class PromptResponse {

    /**
     * プロンプト判定の結果です。
     * プロンプトが安全かつ有効と見なされる場合は {@code true}、それ以外の場合は {@code false} です。
     */
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
