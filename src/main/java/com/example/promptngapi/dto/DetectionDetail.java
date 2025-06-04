package com.example.promptngapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetectionDetail {

    private String type;
    private String matched_pattern;
    private String input_substring;
    private Double similarity_score; // Use Double to allow null
    private String details;
    private String original_text_full; // New field

    /**
     * Default constructor for Jackson deserialization.
     */
    public DetectionDetail() {
    }

    // Constructor
    public DetectionDetail(String type, String matched_pattern, String input_substring, Double similarity_score, String details, String original_text_full) {
        this.type = type;
        this.matched_pattern = matched_pattern;
        this.input_substring = input_substring;
        this.similarity_score = similarity_score;
        this.details = details;
        this.original_text_full = original_text_full;
    }

    // Constructor without similarity_score, details, and original_text_full for simpler cases (or adapt as needed)
    public DetectionDetail(String type, String matched_pattern, String input_substring) {
        this(type, matched_pattern, input_substring, null, null, null);
    }

    // Constructor for cases where original_text_full is needed but maybe not similarity/details
    public DetectionDetail(String type, String matched_pattern, String input_substring, String original_text_full) {
        this(type, matched_pattern, input_substring, null, null, original_text_full);
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

    public String getOriginal_text_full() {
        return original_text_full;
    }

    public void setOriginal_text_full(String original_text_full) {
        this.original_text_full = original_text_full;
    }
}
