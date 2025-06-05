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

    private Double similarityThreshold;
    private Integer nonJapaneseSentenceWordThreshold;

    // Default constructor
    public PromptRequest() {
    }

    // Constructor with fields
    public PromptRequest(String text) {
        this.text = text;
        this.similarityThreshold = null;
        this.nonJapaneseSentenceWordThreshold = null;
    }

    public PromptRequest(String text, Double similarityThreshold, Integer nonJapaneseSentenceWordThreshold) {
        this.text = text;
        this.similarityThreshold = similarityThreshold;
        this.nonJapaneseSentenceWordThreshold = nonJapaneseSentenceWordThreshold;
    }

    // Getter
    public String getText() {
        return text;
    }

    // Setter
    public void setText(String text) {
        this.text = text;
    }

    // Getters and Setters for new fields
    public Double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(Double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public Integer getNonJapaneseSentenceWordThreshold() {
        return nonJapaneseSentenceWordThreshold;
    }

    public void setNonJapaneseSentenceWordThreshold(Integer nonJapaneseSentenceWordThreshold) {
        this.nonJapaneseSentenceWordThreshold = nonJapaneseSentenceWordThreshold;
    }
}
