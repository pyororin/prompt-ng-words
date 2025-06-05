package com.example.promptngapi.dto;

import java.util.ArrayList;
import java.util.List;

public class PromptNGResponse {

    private boolean overall_result;
    private List<DetectionDetail> detections;
    private Double similarityThreshold;
    private Integer nonJapaneseSentenceWordThreshold;

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
