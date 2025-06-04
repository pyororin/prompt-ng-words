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

    @NotNull
    private Double similarityThreshold = 0.7; // Jaro-Winkler類似度チェックの閾値 (デフォルト値)

    @NotNull
    private Integer nonJapaneseSentenceWordThreshold = 3; // 非日本語の文章と判定するための単語数の閾値 (デフォルト値)

    public Double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public Integer getNonJapaneseSentenceWordThreshold() {
        return nonJapaneseSentenceWordThreshold;
    }

    public void setNonJapaneseSentenceWordThreshold(Integer nonJapaneseSentenceWordThreshold) {
        this.nonJapaneseSentenceWordThreshold = nonJapaneseSentenceWordThreshold;
    }
}
