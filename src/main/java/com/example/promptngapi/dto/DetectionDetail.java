package com.example.promptngapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetectionDetail {

    private String type;
    private String matched_pattern;
    private String input_substring;
    private Double similarity_score; // Use Double to allow null
    private String details;

    /**
     * Default constructor for Jackson deserialization.
     */
    public DetectionDetail() {
    }

    // Constructor
    public DetectionDetail(String type, String matched_pattern, String input_substring, Double similarity_score, String details) {
        this.type = type;
        this.matched_pattern = matched_pattern;
        this.input_substring = input_substring;
        this.similarity_score = similarity_score;
        this.details = details;
    }

    // Constructor without similarity_score and details for simpler cases
    public DetectionDetail(String type, String matched_pattern, String input_substring) {
        this(type, matched_pattern, input_substring, null, null);
    }


    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMatched_pattern() {
        return matched_pattern;
    }

    public void setMatched_pattern(String matched_pattern) {
        this.matched_pattern = matched_pattern;
    }

    public String getInput_substring() {
        return input_substring;
    }

    public void setInput_substring(String input_substring) {
        this.input_substring = input_substring;
    }

    public Double getSimilarity_score() {
        return similarity_score;
    }

    public void setSimilarity_score(Double similarity_score) {
        this.similarity_score = similarity_score;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
