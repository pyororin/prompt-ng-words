package com.example.integrationtester;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TestService {

    private static final String YAML_FILE = "/integration-test.yaml";

    // Keywords that might indicate an NG (inappropriate/harmful) prompt
    private static final Set<String> NG_PROMPT_KEYWORDS = new HashSet<>(Arrays.asList(
            "不適切", "ハック", "脆弱性", "盗む", "パスワード", "無限ループ", "攻撃"
    ));

    // Patterns to detect common PII (Personally Identifiable Information)
    private static final Set<Pattern> PII_PATTERNS = new HashSet<>(Arrays.asList(
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), // Email
            Pattern.compile("(0[57-9]0-\\d{4}-\\d{4})|(0\\d{1,4}-\\d{1,4}-\\d{4})"), // Phone numbers (Japanese formats)
            Pattern.compile("住所は|所在地は"), // Address keywords
            Pattern.compile("クレジットカード番号は|カード番号は"), // Credit card keywords
            Pattern.compile("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b"), // Basic credit card number pattern
            Pattern.compile("マイナンバーカードの番号は|マイナンバーは"), // MyNumber keywords
            Pattern.compile("\\b\\d{12}\\b") // Basic MyNumber card number pattern (12 digits)
    ));

    public List<TestResult> loadAndRunTests() {
        List<TestResult> results = new ArrayList<>();
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass().getResourceAsStream(YAML_FILE);

        if (inputStream == null) {
            System.err.println("YAML file not found: " + YAML_FILE);
            results.add(new TestResult("YAML File Access", "Setup", "failed", "File not found: " + YAML_FILE));
            return results;
        }
        try {
            Map<String, Object> data = yaml.load(inputStream);
            Map<String, Object> integrationTest = (Map<String, Object>) data.get("integration-test");

            if (integrationTest == null) {
                 results.add(new TestResult("YAML Structure", "Setup", "failed", "Missing 'integration-test' root key in YAML."));
                 return results;
            }

            processCategoryGroup(results, (Map<String, List<String>>) integrationTest.get("prompt"), "prompt");
            processCategoryGroup(results, (Map<String, List<String>>) integrationTest.get("personal"), "personal");
        } catch (Exception e) {
            results.add(new TestResult("YAML Parsing", "Setup", "failed", "Error parsing YAML: " + e.getMessage()));
        }
        return results;
    }

    private void processCategoryGroup(List<TestResult> results, Map<String, List<String>> categoryData, String categoryType) {
        if (categoryData == null) {
            System.err.println("Warning: Category data for '" + categoryType + "' is missing in YAML.");
            return;
        }
        categoryData.forEach((subCategoryKey, prompts) -> {
            if (prompts == null) {
                 System.err.println("Warning: Prompts list for '" + categoryType + "." + subCategoryKey + "' is null in YAML.");
                 return;
            }
            String fullCategory = categoryType + "." + subCategoryKey;
            prompts.forEach(prompt -> {
                if (prompt == null || prompt.trim().isEmpty()) {
                    results.add(new TestResult(prompt, fullCategory, "failed", "Prompt is empty or null."));
                    return;
                }
                evaluatePrompt(results, prompt, fullCategory);
            });
        });
    }

    private void evaluatePrompt(List<TestResult> results, String prompt, String fullCategory) {
        boolean containsNgKeyword = NG_PROMPT_KEYWORDS.stream().anyMatch(prompt::contains);
        boolean containsPiiPattern = PII_PATTERNS.stream().anyMatch(p -> p.matcher(prompt).find());

        String status = "failed"; // Default to failed, meaning logic didn't behave as expected for category
        String reason = "";

        switch (fullCategory) {
            case "prompt.ok":
                if (!containsNgKeyword && !containsPiiPattern) {
                    status = "passed";
                    reason = "OK: No NG keywords or PII patterns found.";
                } else {
                    reason = "FAIL: Expected prompt.ok. Found: " + (containsNgKeyword ? "NG keyword. " : "") + (containsPiiPattern ? "PII pattern." : "");
                }
                break;
            case "prompt.ng":
                if (containsNgKeyword) {
                    status = "passed";
                    reason = "OK: NG keyword detected as expected.";
                } else {
                    reason = "FAIL: Expected prompt.ng. No NG keyword found.";
                }
                // Optional: check for PII as well if that's a concern for ng prompts
                // if (containsPiiPattern) reason += " Also contains PII.";
                break;
            case "personal.ok":
                // For personal.ok, it should ideally not contain strong PII, and also not be an NG prompt.
                if (!containsPiiPattern && !containsNgKeyword) {
                    status = "passed";
                    reason = "OK: No specific PII patterns or NG keywords detected.";
                } else {
                    reason = "FAIL: Expected personal.ok. Found: " + (containsPiiPattern ? "PII pattern. " : "") + (containsNgKeyword ? "NG keyword." : "");
                }
                break;
            case "personal.ng":
                if (containsPiiPattern) {
                    status = "passed";
                    reason = "OK: PII pattern detected as expected.";
                } else {
                    reason = "FAIL: Expected personal.ng. No PII pattern detected.";
                }
                // Optional: check for NG keywords if that's a concern
                // if (containsNgKeyword) reason += " Also contains NG keyword.";
                break;
            default:
                reason = "Unknown category: " + fullCategory;
                break;
        }
        results.add(new TestResult(prompt, fullCategory, status, reason));
    }
}
