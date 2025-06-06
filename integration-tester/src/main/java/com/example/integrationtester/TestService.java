package com.example.integrationtester;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TestService {

    private static final String YAML_FILE = "/integration-test.yaml"; // Path within resources

    public List<TestResult> loadAndRunTests() {
        List<TestResult> results = new ArrayList<>();
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass().getResourceAsStream(YAML_FILE);

        if (inputStream == null) {
            System.err.println("YAML file not found in classpath: " + YAML_FILE);
            // Add a specific error result to be included in the report
            results.add(new TestResult("YAML File Loading", "Setup", "failed", "File not found in classpath: " + YAML_FILE + ". Ensure it's in src/main/resources."));
            return results;
        }

        Map<String, Object> data = null;
        try {
            data = yaml.load(inputStream);
        } catch (Exception e) {
             System.err.println("Error parsing YAML file: " + e.getMessage());
             results.add(new TestResult("YAML File Parsing", "Setup", "failed", "Error parsing YAML: " + e.getMessage()));
             return results;
        }


        if (data == null || !(data.get("integration-test") instanceof Map)) {
            System.err.println("YAML file is malformed or missing 'integration-test' root map.");
            results.add(new TestResult("YAML Structure", "Setup", "failed", "YAML is malformed or missing 'integration-test' root map."));
            return results;
        }

        Map<String, Object> integrationTest = (Map<String, Object>) data.get("integration-test");

        // Defensive casting and checking
        Object promptObj = integrationTest.get("prompt");
        if (promptObj instanceof Map) {
            processCategory(results, (Map<String, List<String>>) promptObj, "prompt");
        } else {
            System.err.println("Warning: 'prompt' category data is missing or not a map.");
        }

        Object personalObj = integrationTest.get("personal");
        if (personalObj instanceof Map) {
            processCategory(results, (Map<String, List<String>>) personalObj, "personal");
        } else {
            System.err.println("Warning: 'personal' category data is missing or not a map.");
        }

        return results;
    }

    private void processCategory(List<TestResult> results, Map<String, List<String>> categoryData, String categoryType) {
        if (categoryData == null) {
            System.err.println("Warning: No data found for category type: " + categoryType);
            return;
        }

        // Handle 'ok' sub-category
        List<String> okPrompts = categoryData.get("ok");
        if (okPrompts != null) {
            if (okPrompts instanceof List) {
                for (String prompt : okPrompts) {
                    runSingleTest(results, prompt, categoryType + ".ok");
                }
            } else {
                 System.err.println("Warning: 'ok' sub-category in '" + categoryType + "' is not a list.");
            }
        } else {
            System.err.println("Warning: Missing 'ok' sub-category in '" + categoryType + "'.");
        }

        // Handle 'ng' sub-category
        List<String> ngPrompts = categoryData.get("ng");
        if (ngPrompts != null) {
            if (ngPrompts instanceof List) {
                for (String prompt : ngPrompts) {
                    runSingleTest(results, prompt, categoryType + ".ng");
                }
            } else {
                System.err.println("Warning: 'ng' sub-category in '" + categoryType + "' is not a list.");
            }
        } else {
            System.err.println("Warning: Missing 'ng' sub-category in '" + categoryType + "'.");
        }
    }

    private void runSingleTest(List<TestResult> results, String prompt, String fullCategoryName) {
        // Simulate test: for now, just check if prompt is not empty and is a string
        if (prompt != null && !prompt.trim().isEmpty() && prompt instanceof String) {
            results.add(new TestResult(prompt, fullCategoryName, "passed", null));
        } else {
            results.add(new TestResult(String.valueOf(prompt), fullCategoryName, "failed", "Prompt is empty, null, or not a string."));
        }
    }
}
