package com.example.promptngapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;

@SpringBootTest(classes = PromptInjectionRuleConfig.class)
@ActiveProfiles("test") // Ensure this doesn't conflict with main app context if not needed
public class PromptInjectionRuleConfigTest {

    @Autowired
    private PromptInjectionRuleConfig ruleConfig;

    @Test
    void testForbiddenWordsJpLoaded() {
        List<String> forbiddenWords = ruleConfig.getForbiddenWordsJp();
        assertThat(forbiddenWords).isNotNull();
        assertThat(forbiddenWords).isNotEmpty();
        // Check for a few specific words known to be in the YAML
        assertThat(forbiddenWords).contains("パスワード", "クレカ", "機密", "社外秘");
        System.out.println("Loaded Japanese forbidden words: " + forbiddenWords.size());
    }

    @Test
    void testEnglishInjectionPatternsLoaded() {
        Map<String, List<String>> enPatterns = ruleConfig.getEnglishInjectionPatterns();
        assertThat(enPatterns).isNotNull();
        assertThat(enPatterns).isNotEmpty();
        // Check for the "general_english" category and a pattern within it
        assertThat(enPatterns.get("general_english")).isNotNull();
        assertThat(enPatterns.get("general_english")).contains("ignore previous instructions"); // Removed period, match case
        System.out.println("Loaded English injection pattern categories: " + enPatterns.size());
        enPatterns.forEach((key, value) -> System.out.println("  " + key + ": " + value.size() + " patterns"));
    }

    @Test
    void testJapaneseInjectionPatternsLoaded() {
        Map<String, List<String>> jaPatterns = ruleConfig.getJapaneseInjectionPatterns();
        assertThat(jaPatterns).isNotNull();
        assertThat(jaPatterns).isNotEmpty();
        // Check for the "general_japanese" category and a pattern within it
        assertThat(jaPatterns.get("general_japanese")).isNotNull();
        assertThat(jaPatterns.get("general_japanese")).contains("以前の指示を無視して");
        System.out.println("Loaded Japanese injection pattern categories: " + jaPatterns.size());
        jaPatterns.forEach((key, value) -> System.out.println("  " + key + ": " + value.size() + " patterns"));

    }

    @Test
    void testAllPatternStringsLoaded() {
        List<String> allPatterns = ruleConfig.getAllPatternStrings();
        assertThat(allPatterns).isNotNull();
        assertThat(allPatterns).isNotEmpty();
        // Check for a few patterns from different languages/categories
        assertThat(allPatterns).contains("ignore previous instructions", "以前の指示を無視して");
        System.out.println("Total pattern strings loaded: " + allPatterns.size());
    }
}
