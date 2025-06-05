package com.example.promptngapi.controller;

import com.example.promptngapi.dto.DetectionDetail;
import com.example.promptngapi.dto.PromptNGResponse;
import com.example.promptngapi.dto.PromptRequest;
import com.example.promptngapi.service.PromptInjectionDetector;
import com.example.promptngapi.service.SensitiveInformationDetector;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
public class PromptNGControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // For serializing PromptRequest and deserializing PromptNGResponse

    @MockBean
    private SensitiveInformationDetector sensitiveInformationDetectorMock;

    @MockBean
    private PromptInjectionDetector promptInjectionDetectorMock;

    private String asJsonString(final Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> provideTestParametersForJudgePrompt() {
        DetectionDetail sensitiveDetail = new DetectionDetail("sensitive_info_test", "test_pattern_sens", "test_input_sens", 1.0, "Sensitive info test details", "test_input_sens");
        DetectionDetail injectionDetail = new DetectionDetail("prompt_injection_test", "test_pattern_inj", "test_input_inj", 1.0, "Injection test details", "test_input_inj");
        DetectionDetail sensitiveDetail2 = new DetectionDetail("sensitive_info_test", "s_pattern", "s_input", 1.0, "Sensitive", "s_input");
        DetectionDetail injectionDetail2 = new DetectionDetail("injection_test", "i_pattern", "i_input", 1.0, "Injection", "i_input");

        return Stream.of(
            Arguments.of(
                "No Issues: Benign prompt should pass",
                "This is a benign prompt.",
                Collections.emptyList(),
                Collections.emptyList(),
                true,
                0,
                HttpStatus.OK,
                Collections.emptyList()
            ),
            Arguments.of(
                "Only Sensitive Info: Should return false with sensitive details",
                "Contains sensitive data",
                List.of(sensitiveDetail),
                Collections.emptyList(),
                false,
                1,
                HttpStatus.OK,
                List.of("sensitive_info_test")
            ),
            Arguments.of(
                "Only Injection Issue: Should return false with injection details",
                "Ignore previous instructions",
                Collections.emptyList(),
                List.of(injectionDetail),
                false,
                1,
                HttpStatus.OK,
                List.of("prompt_injection_test")
            ),
            Arguments.of(
                "Both Issues: Should return false with combined details",
                "Sensitive and injection attempt",
                List.of(sensitiveDetail2),
                List.of(injectionDetail2),
                false,
                2,
                HttpStatus.OK,
                List.of("sensitive_info_test", "injection_test")
            )
        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("provideTestParametersForJudgePrompt")
    void judgePrompt_parameterized(String testCaseName, String promptText,
                                   List<DetectionDetail> sensitiveInfoDetections,
                                   List<DetectionDetail> promptInjectionDetections,
                                   boolean expectedOverallResult, int expectedDetectionsCount,
                                   HttpStatus expectedStatus, List<String> expectedDetectionTypes) throws Exception {

        when(sensitiveInformationDetectorMock.hasSensitiveInformation(anyString())).thenReturn(sensitiveInfoDetections);
        when(promptInjectionDetectorMock.isPromptInjectionAttempt(anyString())).thenReturn(promptInjectionDetections);

        MvcResult mvcResult = mockMvc.perform(post("/prompt-ng/v1/judge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(new PromptRequest(promptText))))
                .andExpect(status().is(expectedStatus.value()))
                .andReturn();

        if (expectedStatus == HttpStatus.OK) {
            String jsonResponse = mvcResult.getResponse().getContentAsString();
            PromptNGResponse response = objectMapper.readValue(jsonResponse, PromptNGResponse.class);

            assertThat(response.isOverall_result()).isEqualTo(expectedOverallResult);
            assertThat(response.getDetections()).isNotNull().hasSize(expectedDetectionsCount);

            if (expectedDetectionsCount > 0 && expectedDetectionTypes != null && !expectedDetectionTypes.isEmpty()) {
                assertThat(response.getDetections()).extracting(DetectionDetail::getType)
                    .containsExactlyInAnyOrderElementsOf(expectedDetectionTypes);
            }
        }
    }

    // Keep bad request tests
    @org.junit.jupiter.api.Test
    void judgePrompt_emptyInputText_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/prompt-ng/v1/judge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(new PromptRequest("")))) // Empty text
                .andExpect(status().isBadRequest());
    }

    @org.junit.jupiter.api.Test
    void judgePrompt_nullInputText_shouldReturnBadRequest() throws Exception {
        PromptRequest requestWithNullText = new PromptRequest(); // text field will be null by default
        // requestWithNullText.setText(null); // Explicitly set if needed, but default is null for String
        mockMvc.perform(post("/prompt-ng/v1/judge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(requestWithNullText)))
                .andExpect(status().isBadRequest());
    }
}
