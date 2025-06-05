package com.example.promptngapi.controller;

import com.example.promptngapi.config.ScoreThresholdsConfig;
import com.example.promptngapi.dto.DetectionDetail;
import com.example.promptngapi.dto.PromptNGResponse;
import com.example.promptngapi.dto.PromptRequest;
import com.example.promptngapi.service.PromptInjectionDetector;
import com.example.promptngapi.service.SensitiveInformationDetector;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prompt Negative Guard APIのコントローラーです。
 * 機密情報や潜在的なインジェクション攻撃についてテキストプロンプトを判定するエンドポイントを提供します。
 */
@RestController
@RequestMapping("/prompt-ng/v1")
@Tag(name = "プロンプト判定API", description = "機密情報やプロンプトインジェクションの試みをテキストプロンプトから検出するAPI")
public class PromptNGController {

    private final SensitiveInformationDetector sensitiveInformationDetector;
    private final PromptInjectionDetector promptInjectionDetector;
    private final ScoreThresholdsConfig scoreThresholdsConfig;

    @Autowired
    public PromptNGController(SensitiveInformationDetector sensitiveInformationDetector,
                              PromptInjectionDetector promptInjectionDetector,
                              ScoreThresholdsConfig scoreThresholdsConfig) {
        this.sensitiveInformationDetector = sensitiveInformationDetector;
        this.promptInjectionDetector = promptInjectionDetector;
        this.scoreThresholdsConfig = scoreThresholdsConfig;
    }

    /**
     * 提供されたテキストに対し、機密情報およびプロンプトインジェクションの試みを判定します。
     *
     * @param request 判定対象のテキストを含むリクエストボディ。バリデーションされます。
     * @return {@link PromptNGResponse} を含む {@link ResponseEntity}。
     *         レスポンスには、総合的な判定結果 (overall_result) と、検出された問題の詳細 (detections) が含まれます。
     *         問題が見つからない場合は overall_result が {@code true} に、検出リストは空になります。
     *         問題が検出された場合は overall_result が {@code false} に、検出リストに詳細が含まれます。
     */
    @PostMapping("/judge")
    @Operation(summary = "プロンプト判定", description = "提供されたテキストに対し、機密情報およびプロンプトインジェクションの試みを判定します。")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "判定成功",
            content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = PromptNGResponse.class))),
        @ApiResponse(responseCode = "400", description = "リクエスト不正 (例: textフィールドが空)",
            content = @Content)
    })
    public ResponseEntity<PromptNGResponse> judgePrompt(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "判定対象のテキストとオプションの閾値を含むリクエストボディ", required = true,
            content = @Content(schema = @Schema(implementation = PromptRequest.class)))
        @Valid @org.springframework.web.bind.annotation.RequestBody PromptRequest request) {
        String inputText = request.getText();
        Double requestSimilarityThreshold = request.getSimilarityThreshold();
        Integer requestNonJapaneseSentenceWordThreshold = request.getNonJapaneseSentenceWordThreshold();

        List<DetectionDetail> injectionIssues;
        List<DetectionDetail> sensitiveInfoIssues;
        List<DetectionDetail> allDetectedIssues = new ArrayList<>();

        Double effectiveSimilarityThreshold;
        Integer effectiveNonJapaneseSentenceWordThreshold;

        try {
            if (requestSimilarityThreshold != null) {
                ScoreThresholdsConfig.setRequestSimilarityThreshold(requestSimilarityThreshold);
            }
            if (requestNonJapaneseSentenceWordThreshold != null) {
                ScoreThresholdsConfig.setRequestNonJapaneseSentenceWordThreshold(requestNonJapaneseSentenceWordThreshold);
            }

            // Perform detections using the potentially overridden thresholds
            injectionIssues = promptInjectionDetector.isPromptInjectionAttempt(inputText);
            sensitiveInfoIssues = sensitiveInformationDetector.hasSensitiveInformation(inputText);

            // Get the actual thresholds used for this request
            effectiveSimilarityThreshold = scoreThresholdsConfig.getSimilarityThreshold();
            effectiveNonJapaneseSentenceWordThreshold = scoreThresholdsConfig.getNonJapaneseSentenceWordThreshold();

        } finally {
            ScoreThresholdsConfig.clearRequestThresholds();
        }

        if (injectionIssues != null) {
            allDetectedIssues.addAll(injectionIssues);
        }
        if (sensitiveInfoIssues != null) {
            allDetectedIssues.addAll(sensitiveInfoIssues);
        }

        boolean overallOk = allDetectedIssues.isEmpty();

        PromptNGResponse response = new PromptNGResponse(overallOk, allDetectedIssues, effectiveSimilarityThreshold, effectiveNonJapaneseSentenceWordThreshold);

        return ResponseEntity.ok(response);
    }
}
