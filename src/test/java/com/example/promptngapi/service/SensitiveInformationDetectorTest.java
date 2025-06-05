package com.example.promptngapi.service;

import com.example.promptngapi.dto.DetectionDetail;
import org.junit.jupiter.api.BeforeEach;
import com.example.promptngapi.dto.DetectionDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class SensitiveInformationDetectorTest {

    private SensitiveInformationDetector detector;

    static record ExpectedSensitiveDetection(String type, String matchedPattern, String inputSubstring, Double score) {
        ExpectedSensitiveDetection(String type, String matchedPattern, String inputSubstring) {
            this(type, matchedPattern, inputSubstring, 1.0); // Default score
        }

        static ExpectedSensitiveDetection fromDetectionDetail(DetectionDetail detail) {
            // Ensure score is non-null for comparison, defaulting if necessary, or handle nulls if they are significant
            Double score = detail.getSimilarity_score(); // Assuming score is on similarity_score field
            return new ExpectedSensitiveDetection(detail.getType(), detail.getMatched_pattern(), detail.getInput_substring(), score != null ? score : 1.0);
        }
    }

    @BeforeEach
    void setUp() {
        detector = new SensitiveInformationDetector();
    }

    static Stream<Arguments> sensitiveInfoTestCases() {
        return Stream.of(
            Arguments.of("Valid Visa16", "My card is 4556739871695869 today",
                List.of(new ExpectedSensitiveDetection("sensitive_info_credit_card", "Credit Card Pattern", "4556739871695869", 1.0))),
            Arguments.of("Visa with Hyphens", "Card: 4556-7398-7169-5869.",
                List.of(new ExpectedSensitiveDetection("sensitive_info_credit_card", "Credit Card Pattern", "4556739871695869", 1.0))),
            Arguments.of("Multiple Credit Cards", "Visa: 4556739871695869 and Mastercard: 5100112233445566",
                List.of(
                    new ExpectedSensitiveDetection("sensitive_info_credit_card", "Credit Card Pattern", "4556739871695869", 1.0),
                    new ExpectedSensitiveDetection("sensitive_info_credit_card", "Credit Card Pattern", "5100112233445566", 1.0)
                )),
            Arguments.of("Invalid Credit Card Length", "My card is 123456789012345 (invalid length)", Collections.emptyList()),
            Arguments.of("Text Without Credit Card", "This is a string with no card numbers.", Collections.emptyList()),

            Arguments.of("Valid MyNumber", "My number is 123456789012, please confirm.",
                List.of(new ExpectedSensitiveDetection("sensitive_info_my_number", "My Number Pattern", "123456789012", 1.0))),
            Arguments.of("MyNumber with Spaces", "My number is 1234 5678 9012 formatted.",
                List.of(new ExpectedSensitiveDetection("sensitive_info_my_number", "My Number Pattern", "123456789012", 1.0))),
            Arguments.of("MyNumber Too Long", "1234567890123 is too long", Collections.emptyList()),
            Arguments.of("MyNumber Too Short", "12345678901 is too short", Collections.emptyList()),

            Arguments.of("Japanese Address Placeholder", "Address: Test Ken Test Shi 1-2-3",
                List.of(new ExpectedSensitiveDetection("sensitive_info_address", "Japanese Address Placeholder Pattern", "該当箇所 (簡易検出のため特定困難)", 0.7))),
            Arguments.of("No Japanese Address", "This is a normal sentence without address cues", Collections.emptyList()),

            Arguments.of("Japanese Name Placeholder", "My friend Test Name Sama is here.",
                List.of(new ExpectedSensitiveDetection("sensitive_info_name", "Japanese Name Placeholder Pattern", "該当箇所 (簡易検出のため特定困難)", 0.7))),
            Arguments.of("No Japanese Name", "This text has no names like that.", Collections.emptyList()),

            Arguments.of("Multiple Mixed Detections", "My card is 4556739871695869, MyNumber is 123456789012, and I live at Test Ken Test Shi 1-2-3, said Test Name Sama.",
                List.of(
                    new ExpectedSensitiveDetection("sensitive_info_credit_card", "Credit Card Pattern", "4556739871695869", 1.0),
                    new ExpectedSensitiveDetection("sensitive_info_my_number", "My Number Pattern", "123456789012", 1.0),
                    new ExpectedSensitiveDetection("sensitive_info_address", "Japanese Address Placeholder Pattern", "該当箇所 (簡易検出のため特定困難)", 0.7), // Corrected score
                    new ExpectedSensitiveDetection("sensitive_info_name", "Japanese Name Placeholder Pattern", "該当箇所 (簡易検出のため特定困難)", 0.7)  // Corrected substring and score
                )),
            Arguments.of("No Sensitive Info", "This is a perfectly normal and safe sentence.", Collections.emptyList()),
            Arguments.of("Empty String", "", Collections.emptyList()),
            Arguments.of("Null Input", null, Collections.emptyList())
        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("sensitiveInfoTestCases")
    void testSensitiveInformationDetectionScenarios(String testCaseName, String inputText, List<ExpectedSensitiveDetection> expectedDetectionsList) {
        List<DetectionDetail> actualDetails = detector.hasSensitiveInformation(inputText);
        assertThat(actualDetails).isNotNull();

        if (expectedDetectionsList.isEmpty()) {
            assertThat(actualDetails).isEmpty();
        } else {
            // Transform actual DetectionDetail objects to ExpectedSensitiveDetection for comparison
            List<ExpectedSensitiveDetection> actualTransformed = actualDetails.stream()
                .map(ExpectedSensitiveDetection::fromDetectionDetail)
                .collect(Collectors.toList());

            assertThat(actualTransformed).containsExactlyInAnyOrderElementsOf(expectedDetectionsList);
        }
    }
}
