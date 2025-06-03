package com.example.promptngapi.dto;

import java.util.ArrayList;
import java.util.List;

public class PromptNGResponse {

    private boolean overall_result;
    private List<DetectionDetail> detections;

    // Constructors
    public PromptNGResponse(boolean overall_result) {
        this.overall_result = overall_result;
        this.detections = new ArrayList<>(); // Initialize to empty list
    }

    public PromptNGResponse(boolean overall_result, List<DetectionDetail> detections) {
        this.overall_result = overall_result;
        this.detections = detections == null ? new ArrayList<>() : detections;
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

    // Helper method to add a detection
    public void addDetection(DetectionDetail detail) {
        if (this.detections == null) {
            this.detections = new ArrayList<>();
        }
        this.detections.add(detail);
    }
}
