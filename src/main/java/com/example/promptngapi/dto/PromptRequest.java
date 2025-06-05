package com.example.promptngapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * プロンプト判定APIのリクエストボディを表します。
 */
@Schema(description = "プロンプト判定APIのリクエストボディ")
public class PromptRequest {

    /**
     * 判定対象のテキストコンテンツです。
     * 空白であってはなりません。
     */
    @Schema(description = "判定対象のテキストコンテンツ。空白であってはなりません。", requiredMode = Schema.RequiredMode.REQUIRED, example = "これはテスト用のプロンプトです。")
    @NotBlank(message = "Text cannot be blank")
    private String text;

    @Schema(description = "プロンプトインジェクション判定のための類似度閾値。0.0から1.0の間。指定しない場合はapplication.yamlまたはデフォルト値が使用されます。", example = "0.85", nullable = true)
    private Double similarityThreshold;

    @Schema(description = "プロンプトインジェクション判定のための非日本語文の単語数閾値。指定しない場合はapplication.yamlまたはデフォルト値が使用されます。", example = "3", nullable = true)
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
