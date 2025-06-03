package com.example.promptngapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class SensitiveInformationDetectorTest {

    private SensitiveInformationDetector detector;

    @BeforeEach
    void setUp() {
        detector = new SensitiveInformationDetector();
    }

    // --- isCreditCard Tests ---
    @Test
    void testIsCreditCard_validVisa13() { assertTrue(detector.isCreditCard("4556739871695"), "Expected Visa 13 to be valid"); }
    @Test
    void testIsCreditCard_validVisa16() { assertTrue(detector.isCreditCard("4556739871695869"), "Expected Visa 16 to be valid"); }
    @Test
    void testIsCreditCard_validMastercard() { assertTrue(detector.isCreditCard("5100112233445566"), "Expected Mastercard to be valid"); }
    @Test
    void testIsCreditCard_validMastercardNewRange() { assertTrue(detector.isCreditCard("2221002233445566"), "Expected Mastercard new range to be valid"); }
    @Test
    void testIsCreditCard_validAmex15() { assertTrue(detector.isCreditCard("378282246310005"), "Expected Amex 15 to be valid"); }
    @Test
    void testIsCreditCard_validVisaWithHyphens() { assertTrue(detector.isCreditCard("4556-7398-7169-5869"), "Expected Visa with hyphens to be valid"); }
    @Test
    void testIsCreditCard_validMastercardWithSpaces() { assertTrue(detector.isCreditCard("5100 1122 3344 5566"), "Expected Mastercard with spaces to be valid"); }
    @Test
    void testIsCreditCard_validAmexWithSpaces() { assertTrue(detector.isCreditCard("3782 822463 10005"), "Expected Amex with spaces to be valid"); }

    @Test
    void testIsCreditCard_invalidShortVisa() {
         assertFalse(detector.isCreditCard("49927398716"), "Expected 49927398716 (Visa 11-digit) to be invalid");
    }

    @Test
    void testIsCreditCard_invalidPrefix() { assertFalse(detector.isCreditCard("1234567890123456"), "Expected invalid prefix to be invalid"); }
    @Test
    void testIsCreditCard_invalidVisa15() { assertFalse(detector.isCreditCard("455673987169586"), "Expected Visa-like 15 digits to be invalid"); }
    @Test
    void testIsCreditCard_invalidMastercard15() { assertFalse(detector.isCreditCard("510011223344556"), "Expected Mastercard-like 15 digits to be invalid"); }
    @Test
    void testIsCreditCard_invalidAmex14() { assertFalse(detector.isCreditCard("37828224631000"), "Expected Amex-like 14 digits to be invalid"); }
    @Test
    void testIsCreditCard_invalidString() { assertFalse(detector.isCreditCard("teststring"), "Expected 'teststring' to be invalid"); }
    @Test
    void testIsCreditCard_emptyString() { assertFalse(detector.isCreditCard(""), "Expected empty string to be invalid"); }
    @Test
    void testIsCreditCard_nullString() { assertFalse(detector.isCreditCard(null), "Expected null string to be invalid"); }

    // --- isMyNumber Tests ---
    @Test
    void testIsMyNumber_valid1() { assertTrue(detector.isMyNumber("123456789012"), "Expected valid My Number"); }
    @Test
    void testIsMyNumber_valid2() { assertTrue(detector.isMyNumber("098765432109"), "Expected valid My Number"); }
    @Test
    void testIsMyNumber_validWithHyphens() { assertTrue(detector.isMyNumber("1234-5678-9012"), "Expected valid My Number with hyphens"); }
    @Test
    void testIsMyNumber_validWithSpaces() { assertTrue(detector.isMyNumber("0987 6543 2109"), "Expected valid My Number with spaces"); }

    @Test
    void testIsMyNumber_invalidTooShort() { assertFalse(detector.isMyNumber("12345678901"), "Expected too short My Number to be invalid"); }
    @Test
    void testIsMyNumber_invalidTooLong() { assertFalse(detector.isMyNumber("1234567890123"), "Expected too long My Number to be invalid"); }
    @Test
    void testIsMyNumber_invalidNonDigits() { assertFalse(detector.isMyNumber("abcdefghijkl"), "Expected non-digit My Number to be invalid"); }
    @Test
    void testIsMyNumber_invalidContainsNonDigit() { assertFalse(detector.isMyNumber("12345678901A"), "Expected My Number with non-digit to be invalid"); }
    @Test
    void testIsMyNumber_emptyString() { assertFalse(detector.isMyNumber(""), "Expected empty string for My Number to be invalid"); }
    @Test
    void testIsMyNumber_nullString() { assertFalse(detector.isMyNumber(null), "Expected null string for My Number to be invalid"); }

    // --- isAddress Tests (Placeholder) ---
    @Test
    void testIsAddress_placeholderPositive() {
        // Based on current simple logic: "Test Ken Test Shi 1-1-1" (Prefecture, Ward, Number)
        assertTrue(detector.isAddress("Test Ken Test Shi 1-2-3"), "Placeholder positive address test failed");
        assertTrue(detector.isAddress("Test Fu Test Shi Kita Ku Umeda 1-3-1"), "Placeholder positive address test failed");
    }

    @Test
    void testIsAddress_placeholderNegative() {
        assertFalse(detector.isAddress("This is just text"), "Placeholder negative address test failed");
        assertFalse(detector.isAddress("Test Ken"), "Placeholder negative address test failed");
        assertFalse(detector.isAddress("Test Ku"), "Placeholder negative address test failed");
        assertFalse(detector.isAddress("1-2-3"), "Placeholder negative address test failed");
    }

    // --- isName Tests (Placeholder) ---
    @Test
    void testIsName_placeholderPositive() {
        // Based on current simple logic: "Test Name Sama"
        assertTrue(detector.isName("Test Name Sama"), "Placeholder positive name test failed");
        assertTrue(detector.isName("Test Kakuei San"), "Placeholder positive name test failed");
        assertTrue(detector.isName("Test Ichiro Dono"), "Placeholder positive name test failed");
    }

    @Test
    void testIsName_placeholderNegative() {
        assertFalse(detector.isName("Test Taro"), "Placeholder negative name test failed (no honorific)");
        assertFalse(detector.isName("Just text"), "Placeholder negative name test failed");
    }

    // --- hasSensitiveInformation Tests ---
    @Test
    void testHasSensitiveInformation_creditCardOnly_exact() {
        assertTrue(detector.hasSensitiveInformation("4556739871695869"), "Failed with exact credit card");
    }

    @Test
    void testHasSensitiveInformation_myNumberOnly_exact() {
        assertTrue(detector.hasSensitiveInformation("123456789012"), "Failed with exact My Number");
    }

    @Test
    void testHasSensitiveInformation_addressOnly_exact() {
        assertTrue(detector.hasSensitiveInformation("Test Ken Test Shi 1-2-3"), "Failed with exact address");
    }

    @Test
    void testHasSensitiveInformation_nameOnly_exact() {
        assertTrue(detector.hasSensitiveInformation("Test Name Sama"), "Failed with exact name");
    }

    @Test
    void testHasSensitiveInformation_multipleTypes_exactNumberAndPlaceholderName() {
        // Test with an exact MyNumber and a name that the placeholder logic should catch.
        // This will fail if isMyNumber also tries to find in "Test Name Sama" or vice-versa in a more complex text.
        // For now, testing one exact match that should make it true.
        assertTrue(detector.hasSensitiveInformation("123456789012 Test Name Sama"), "Failed with multiple types (exact number, placeholder name)");
    }

    @Test
    void testHasSensitiveInformation_creditCardEmbedded_shouldBeFalse() {
        // With strict 'matches()' logic for isCreditCard, embedded numbers are not found by isCreditCard itself.
        assertFalse(detector.hasSensitiveInformation("My credit card is 4556739871695869."), "Should be false for embedded credit card with strict isCreditCard");
    }

    @Test
    void testHasSensitiveInformation_myNumberEmbedded_shouldBeFalse() {
        assertFalse(detector.hasSensitiveInformation("MyNumber: 123456789012"), "Should be false for embedded My Number with strict isMyNumber");
    }


    @Test
    void testHasSensitiveInformation_none() {
        assertFalse(detector.hasSensitiveInformation("This is a perfectly normal sentence."), "Failed with no sensitive info");
    }

    @Test
    void testHasSensitiveInformation_emptyAndNull() {
        assertFalse(detector.hasSensitiveInformation(""), "Failed with empty string");
        assertFalse(detector.hasSensitiveInformation(null), "Failed with null string");
    }
}
