package com.example.promptngapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import jakarta.validation.constraints.NotNull; // Added import

@Configuration
@ConfigurationProperties(prefix = "score-thresholds")
public class ScoreThresholdsConfig {

    @NotNull
    private Double similarityThreshold; // Changed to Double to allow null for validation
    @NotNull
    private Integer nonJapaneseSentenceWordThreshold; // Changed to Integer to allow null for validation

    public Double getSimilarityThreshold() { // Changed return type
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public Integer getNonJapaneseSentenceWordThreshold() { // Changed return type
        return nonJapaneseSentenceWordThreshold;
    }

    public void setNonJapaneseSentenceWordThreshold(Integer nonJapaneseSentenceWordThreshold) { // Changed parameter type
        this.nonJapaneseSentenceWordThreshold = nonJapaneseSentenceWordThreshold;
    }
}
