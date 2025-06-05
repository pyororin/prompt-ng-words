package com.example.promptngapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import jakarta.validation.constraints.NotNull;

/**
 * スコアリングの閾値設定を保持するクラス。
 * これらの設定は `score_thresholds.yaml` ファイルからロードされます。
 */
@Configuration
@ConfigurationProperties(prefix = "score-thresholds")
public class ScoreThresholdsConfig {

    // ThreadLocal storage for request-specific thresholds
    private static final ThreadLocal<Double> requestSimilarityThreshold = new ThreadLocal<>();
    private static final ThreadLocal<Integer> requestNonJapaneseSentenceWordThreshold = new ThreadLocal<>();

    @NotNull
    private Double similarityThreshold; // Jaro-Winkler類似度チェックの閾値

    @NotNull
    private Integer nonJapaneseSentenceWordThreshold; // 非日本語の文章と判定するための単語数の閾値

    public Double getSimilarityThreshold() {
        Double requestSpecificThreshold = requestSimilarityThreshold.get();
        if (requestSpecificThreshold != null) {
            return requestSpecificThreshold;
        }
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public Integer getNonJapaneseSentenceWordThreshold() {
        Integer requestSpecificThreshold = requestNonJapaneseSentenceWordThreshold.get();
        if (requestSpecificThreshold != null) {
            return requestSpecificThreshold;
        }
        return nonJapaneseSentenceWordThreshold;
    }

    public void setNonJapaneseSentenceWordThreshold(Integer nonJapaneseSentenceWordThreshold) {
        this.nonJapaneseSentenceWordThreshold = nonJapaneseSentenceWordThreshold;
    }

    // Methods to manage request-specific thresholds
    public static void setRequestSimilarityThreshold(Double threshold) {
        if (threshold != null) {
            requestSimilarityThreshold.set(threshold);
        } else {
            requestSimilarityThreshold.remove();
        }
    }

    public static void setRequestNonJapaneseSentenceWordThreshold(Integer threshold) {
        if (threshold != null) {
            requestNonJapaneseSentenceWordThreshold.set(threshold);
        } else {
            requestNonJapaneseSentenceWordThreshold.remove();
        }
    }

    public static void clearRequestThresholds() {
        requestSimilarityThreshold.remove();
        requestNonJapaneseSentenceWordThreshold.remove();
    }
}
