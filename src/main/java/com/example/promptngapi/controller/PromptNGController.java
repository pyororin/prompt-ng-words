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
