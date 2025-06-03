package com.example.promptngapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PromptInjectionRuleConfig {

    private static final Logger logger = LoggerFactory.getLogger(PromptInjectionRuleConfig.class);

    @Value("classpath:prompt_injection_rules.yaml")
    private Resource rulesFile;

    private List<String> forbiddenWordsJp = new ArrayList<>();
    private Map<String, List<String>> englishInjectionPatterns = Collections.emptyMap();
    private Map<String, List<String>> japaneseInjectionPatterns = Collections.emptyMap();
    private List<String> allPatternStrings = new ArrayList<>();


    @PostConstruct
    public void loadRules() {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = rulesFile.getInputStream()) {
            Map<String, Object> yamlData = yaml.load(inputStream);
            if (yamlData == null) {
                logger.warn("Prompt injection rules file is empty or could not be parsed: {}", rulesFile.getFilename());
                return;
            }

            // Load forbidden_words_jp
            List<String> loadedForbiddenWords = (List<String>) yamlData.get("forbidden_words_jp");
            if (loadedForbiddenWords != null) {
                this.forbiddenWordsJp = loadedForbiddenWords;
                logger.info("Loaded {} Japanese forbidden words.", loadedForbiddenWords.size());
            } else {
                logger.warn("No 'forbidden_words_jp' found in rules file.");
            }

            // Load injection_patterns
            Map<String, Object> patternsMap = (Map<String, Object>) yamlData.get("injection_patterns");
            if (patternsMap != null) {
                this.englishInjectionPatterns = parseLanguagePatterns(patternsMap, "english", "en");
                this.japaneseInjectionPatterns = parseLanguagePatterns(patternsMap, "japanese", "ja");

                // Combine all pattern strings for PromptInjectionDetector
                this.englishInjectionPatterns.values().forEach(allPatternStrings::addAll);
                this.japaneseInjectionPatterns.values().forEach(allPatternStrings::addAll);
                logger.info("Loaded {} English pattern categories and {} Japanese pattern categories.",
                            englishInjectionPatterns.size(), japaneseInjectionPatterns.size());
                logger.info("Total of {} pattern strings loaded for PromptInjectionDetector.", allPatternStrings.size());

            } else {
                logger.warn("No 'injection_patterns' found in rules file.");
            }

        } catch (Exception e) {
            logger.error("Failed to load or parse prompt injection rules from: {}", rulesFile.getFilename(), e);
            // Keep default empty lists/maps in case of error
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> parseLanguagePatterns(Map<String, Object> patternsMap, String langKeyPrimary, String langKeySecondary) {
        Map<String, Object> langPatternsObj = (Map<String, Object>) patternsMap.get(langKeyPrimary);
        if (langPatternsObj == null && langKeySecondary != null) {
             langPatternsObj = (Map<String, Object>) patternsMap.get(langKeySecondary);
        }

        if (langPatternsObj != null) {
            return langPatternsObj.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof List)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> (List<String>) entry.getValue(),
                    (existing, replacement) -> existing // merge function, just in case of duplicate keys
                ));
        }
        logger.warn("No '{}' or '{}' pattern category found in injection_patterns.", langKeyPrimary, langKeySecondary);
        return Collections.emptyMap();
    }


    public List<String> getForbiddenWordsJp() {
        return Collections.unmodifiableList(forbiddenWordsJp);
    }

    public Map<String, List<String>> getEnglishInjectionPatterns() {
        return Collections.unmodifiableMap(englishInjectionPatterns);
    }

    public Map<String, List<String>> getJapaneseInjectionPatterns() {
        return Collections.unmodifiableMap(japaneseInjectionPatterns);
    }

    /**
     * Provides a flat list of all pattern strings from all languages and categories.
     * This is intended for use by PromptInjectionDetector.
     * @return An unmodifiable list of all pattern strings.
     */
    public List<String> getAllPatternStrings() {
        return Collections.unmodifiableList(allPatternStrings);
    }
}
