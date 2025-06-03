package com.example.promptngapi.service;

import com.example.promptngapi.dto.DetectionDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

public class SensitiveInformationDetectorTest {

    private SensitiveInformationDetector detector;

    @BeforeEach
    void setUp() {
        detector = new SensitiveInformationDetector();
    }

    // --- Credit Card Detection Tests ---
    @Test
    void hasSensitiveInformation_withValidVisa16_shouldReturnCreditCardDetail() {
        List<DetectionDetail> details = detector.hasSensitiveInformation("My card is 4556739871695869 today");
        assertThat(details).isNotNull().hasSize(1);
        DetectionDetail detail = details.get(0);
        assertThat(detail.getType()).isEqualTo("sensitive_info_credit_card");
        assertThat(detail.getMatched_pattern()).isEqualTo("Credit Card Pattern");
        assertThat(detail.getInput_substring()).isEqualTo("4556739871695869");
        assertThat(detail.getSimilarity_score()).isEqualTo(1.0);
    }

    @Test
    void hasSensitiveInformation_withVisaWithHyphens_shouldReturnCreditCardDetail() {
        List<DetectionDetail> details = detector.hasSensitiveInformation("Card: 4556-7398-7169-5869.");
        assertThat(details).isNotNull().hasSize(1);
        DetectionDetail detail = details.get(0);
        assertThat(detail.getType()).isEqualTo("sensitive_info_credit_card");
        assertThat(detail.getInput_substring()).isEqualTo("4556739871695869"); // Cleaned version
    }

    @Test
    void hasSensitiveInformation_withMultipleCreditCards_shouldReturnMultipleDetails() {
        List<DetectionDetail> details = detector.hasSensitiveInformation("Visa: 4556739871695869 and Mastercard: 5100112233445566");
        assertThat(details).isNotNull().hasSize(2);
        assertThat(details).extracting(DetectionDetail::getInput_substring).containsExactlyInAnyOrder("4556739871695869", "5100112233445566");
        assertThat(details).allMatch(d -> d.getType().equals("sensitive_info_credit_card"));
    }

    @Test
    void hasSensitiveInformation_invalidCreditCard_shouldNotDetectAsCard() {
        // This tests that a number that doesn't match STRICT_CREDIT_CARD_PATTERN (after cleaning)
        // but might be found by FIND_IN_CLEANED_CREDIT_CARD_PATTERN if it were less strict, is handled.
        // Current logic uses STRICT_CREDIT_CARD_PATTERN and matches(), so "123456789012345" should not match.
        List<DetectionDetail> details = detector.hasSensitiveInformation("My card is 123456789012345 (invalid length)");
         assertThat(details).allSatisfy(d -> assertThat(d.getType()).isNotEqualTo("sensitive_info_credit_card"));
    }

    @Test
    void hasSensitiveInformation_textWithoutCreditCard_shouldNotReturnCardDetail() {
        List<DetectionDetail> details = detector.hasSensitiveInformation("This is a string with no card numbers.");
        assertThat(details).allSatisfy(d -> assertThat(d.getType()).isNotEqualTo("sensitive_info_credit_card"));
    }

    // --- My Number Detection Tests ---
    @Test
    void hasSensitiveInformation_withMyNumber_shouldReturnMyNumberDetail() {
        // Test with MyNumber embedded in text
        List<DetectionDetail> details = detector.hasSensitiveInformation("My number is 123456789012, please confirm.");
        assertThat(details).isNotNull().hasSize(1);
        DetectionDetail detail = details.get(0);
        assertThat(detail.getType()).isEqualTo("sensitive_info_my_number");
        assertThat(detail.getMatched_pattern()).isEqualTo("My Number Pattern"); // From service
        assertThat(detail.getInput_substring()).isEqualTo("123456789012");
        assertThat(detail.getSimilarity_score()).isEqualTo(1.0);
    }

    @Test
    void hasSensitiveInformation_withMyNumberWithSpaces_shouldReturnMyNumberDetail() {
        List<DetectionDetail> details = detector.hasSensitiveInformation("My number is 1234 5678 9012 formatted.");
        assertThat(details).isNotNull().hasSize(1);
        assertThat(details.get(0).getType()).isEqualTo("sensitive_info_my_number");
        assertThat(details.get(0).getInput_substring()).isEqualTo("123456789012");
    }

    @Test
    void hasSensitiveInformation_myNumberTooLong_shouldNotBeStrictlyMyNumber() {
        // Service cleans and then uses find() with "[0-9]{12}".
        // So, "1234567890123" will have "123456789012" detected.
        List<DetectionDetail> details = detector.hasSensitiveInformation("1234567890123 is too long");
         assertThat(details).isNotNull().hasSize(1);
        assertThat(details.get(0).getInput_substring()).isEqualTo("123456789012");
        assertThat(details.get(0).getType()).isEqualTo("sensitive_info_my_number");
    }

    @Test
    void hasSensitiveInformation_myNumberTooShort_shouldNotDetect() {
        List<DetectionDetail> details = detector.hasSensitiveInformation("12345678901 is too short");
        assertThat(details).allSatisfy(d -> assertThat(d.getType()).isNotEqualTo("sensitive_info_my_number"));
    }


    // --- Address Detection Tests (Placeholder) ---
    @Test
    void hasSensitiveInformation_withJapaneseAddressPlaceholder_shouldReturnAddressDetail() {
        List<DetectionDetail> details = detector.hasSensitiveInformation("Address: Test Ken Test Shi 1-2-3");
        assertThat(details).isNotNull();
        assertThat(details).anySatisfy(detail -> {
            assertThat(detail.getType()).isEqualTo("sensitive_info_address");
            assertThat(detail.getMatched_pattern()).isEqualTo("Japanese Address Placeholder Pattern");
            assertThat(detail.getInput_substring()).isEqualTo("該当箇所 (簡易検出のため特定困難)");
        });
    }

    @Test
    void hasSensitiveInformation_noJapaneseAddress_shouldNotReturnAddressDetail() {
        List<DetectionDetail> details = detector.hasSensitiveInformation("This is a normal sentence without address cues");
        assertThat(details).allSatisfy(d -> assertThat(d.getType()).isNotEqualTo("sensitive_info_address"));
    }

    // --- Name Detection Tests (Placeholder) ---
    @Test
    void hasSensitiveInformation_withJapaneseNamePlaceholder_shouldReturnNameDetail() {
        List<DetectionDetail> details = detector.hasSensitiveInformation("My friend Test Name Sama is here.");
        assertThat(details).isNotNull();
         assertThat(details).anySatisfy(detail -> {
            assertThat(detail.getType()).isEqualTo("sensitive_info_name");
            assertThat(detail.getMatched_pattern()).isEqualTo("Japanese Name Placeholder Pattern");
        });
    }

    @Test
    void hasSensitiveInformation_noJapaneseName_shouldNotReturnNameDetail() {
        List<DetectionDetail> details = detector.hasSensitiveInformation("This text has no names like that.");
         assertThat(details).allSatisfy(d -> assertThat(d.getType()).isNotEqualTo("sensitive_info_name"));
    }

    // --- Combined Tests ---
    @Test
    void hasSensitiveInformation_multipleMixedDetections_shouldReturnAllDetails() {
        String text = "My card is 4556739871695869, MyNumber is 123456789012, and I live at Test Ken Test Shi 1-2-3, said Test Name Sama.";
        List<DetectionDetail> details = detector.hasSensitiveInformation(text);
        assertThat(details).isNotNull().hasSize(4); // Card, MyNumber, Address, Name
        assertThat(details).extracting(DetectionDetail::getType).containsExactlyInAnyOrder(
            "sensitive_info_credit_card",
            "sensitive_info_my_number",
            "sensitive_info_address",
            "sensitive_info_name"
        );
    }

    @Test
    void hasSensitiveInformation_noSensitiveInfo_shouldReturnEmptyList() {
        List<DetectionDetail> details = detector.hasSensitiveInformation("This is a perfectly normal and safe sentence.");
        assertThat(details).isNotNull().isEmpty();
    }

    @Test
    void hasSensitiveInformation_emptyString_shouldReturnEmptyList() {
        List<DetectionDetail> details = detector.hasSensitiveInformation("");
        assertThat(details).isNotNull().isEmpty();
    }

    @Test
    void hasSensitiveInformation_nullInput_shouldReturnEmptyList() {
        List<DetectionDetail> details = detector.hasSensitiveInformation(null);
        assertThat(details).isNotNull().isEmpty();
    }
}
