package com.example.promptngapi.controller;

import com.example.promptngapi.dto.DetectionDetail;
import com.example.promptngapi.dto.PromptNGResponse;
import com.example.promptngapi.dto.PromptRequest;
// import com.example.promptngapi.dto.PromptResponse; // No longer used
import com.example.promptngapi.service.PromptInjectionDetector;
import com.example.promptngapi.service.SensitiveInformationDetector;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prompt Negative Guard APIのコントローラーです。
 * 機密情報や潜在的なインジェクション攻撃についてテキストプロンプトを判定するエンドポイントを提供します。
 */
@RestController
@RequestMapping("/prompt-ng/v1")
public class PromptNGController {

    private final SensitiveInformationDetector sensitiveInformationDetector;
    private final PromptInjectionDetector promptInjectionDetector;

    @Autowired
    public PromptNGController(SensitiveInformationDetector sensitiveInformationDetector,
                              PromptInjectionDetector promptInjectionDetector) {
        this.sensitiveInformationDetector = sensitiveInformationDetector;
        this.promptInjectionDetector = promptInjectionDetector;
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
    public ResponseEntity<PromptNGResponse> judgePrompt(@Valid @RequestBody PromptRequest request) {
        String inputText = request.getText();

        List<DetectionDetail> injectionIssues = promptInjectionDetector.isPromptInjectionAttempt(inputText);
        List<DetectionDetail> sensitiveInfoIssues = sensitiveInformationDetector.hasSensitiveInformation(inputText);

        List<DetectionDetail> allDetectedIssues = new ArrayList<>();
        if (injectionIssues != null) {
            allDetectedIssues.addAll(injectionIssues);
        }
        if (sensitiveInfoIssues != null) {
            allDetectedIssues.addAll(sensitiveInfoIssues);
        }

        boolean overallOk = allDetectedIssues.isEmpty();

        PromptNGResponse response = new PromptNGResponse(overallOk, allDetectedIssues);

        return ResponseEntity.ok(response);
    }
}
