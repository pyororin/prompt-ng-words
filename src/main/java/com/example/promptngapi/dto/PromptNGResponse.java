package com.example.promptngapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "プロンプト判定APIのレスポンス")
public class PromptNGResponse {

    @Schema(description = "総合的な判定結果。問題がなければtrue、何らかの問題が検出されればfalse。", example = "true")
    private boolean overall_result;

    @Schema(description = "検出された問題の詳細リスト。問題がない場合は空のリスト。")
    private List<DetectionDetail> detections;

    @Schema(description = "このリクエストで使用された類似度閾値。", example = "0.85", nullable = true)
    private Double similarityThreshold;

    @Schema(description = "このリクエストで使用された非日本語文の単語数閾値。", example = "3", nullable = true)
    private Integer nonJapaneseSentenceWordThreshold;

    // Default constructor for Jackson
    public PromptNGResponse() {
    }

    // Constructors
    public PromptNGResponse(boolean overall_result) {
        this.overall_result = overall_result;
        this.detections = new ArrayList<>(); // Initialize to empty list
        this.similarityThreshold = null;
        this.nonJapaneseSentenceWordThreshold = null;
    }

    public PromptNGResponse(boolean overall_result, List<DetectionDetail> detections) {
        this.overall_result = overall_result;
        this.detections = detections == null ? new ArrayList<>() : detections;
        this.similarityThreshold = null; // Defaulting to null
        this.nonJapaneseSentenceWordThreshold = null; // Defaulting to null
    }

    public PromptNGResponse(boolean overall_result, List<DetectionDetail> detections, Double similarityThreshold, Integer nonJapaneseSentenceWordThreshold) {
        this.overall_result = overall_result;
        this.detections = detections == null ? new ArrayList<>() : detections;
        this.similarityThreshold = similarityThreshold;
        this.nonJapaneseSentenceWordThreshold = nonJapaneseSentenceWordThreshold;
    }

    // Getters and Setters
    public boolean isOverall_result() {
        return overall_result;
    }

    public void setOverall_result(boolean overall_result) {
        this.overall_result = overall_result;
    }

    public List<DetectionDetail> getDetections() {
        return detections;
    }

    public void setDetections(List<DetectionDetail> detections) {
        this.detections = detections;
    }

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

    // Helper method to add a detection
    public void addDetection(DetectionDetail detail) {
        if (this.detections == null) {
            this.detections = new ArrayList<>();
        }
        this.detections.add(detail);
    }
}
