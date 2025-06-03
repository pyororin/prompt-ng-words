package com.example.promptngapi.controller;

import com.example.promptngapi.dto.PromptRequest;
import com.example.promptngapi.dto.PromptResponse;
import com.example.promptngapi.service.PromptInjectionDetector;
import com.example.promptngapi.service.SensitiveInformationDetector;
import jakarta.validation.Valid;
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
     * @return {@link PromptResponse} を含む {@link ResponseEntity}。
     *         問題が見つからない場合は {@link PromptResponse#result} が {@code true} に、
     *         機密情報またはプロンプトインジェクションの試みが検出された場合は {@code false} になります。
     */
    @PostMapping("/judge")
    public ResponseEntity<PromptResponse> judgePrompt(@Valid @RequestBody PromptRequest request) {
        String inputText = request.getText();

        boolean hasSensitiveInfo = sensitiveInformationDetector.hasSensitiveInformation(inputText);
        boolean isInjectionAttempt = promptInjectionDetector.isPromptInjectionAttempt(inputText);

        if (hasSensitiveInfo || isInjectionAttempt) {
            return ResponseEntity.ok(new PromptResponse(false));
        } else {
            return ResponseEntity.ok(new PromptResponse(true));
        }
    }
}
