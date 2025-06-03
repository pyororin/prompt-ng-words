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
    @SuppressWarnings("unchecked")
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
            // The YAML structure for 'injection_patterns' is now List<Map<String, String>>
            // Each map has "phrase" and "type"
            Object rawPatterns = yamlData.get("injection_patterns");
            if (rawPatterns instanceof List) {
                List<Map<String, String>> loadedPatternItems = (List<Map<String, String>>) rawPatterns;

                List<String> englishPhrases = new ArrayList<>();
                List<String> japanesePhrases = new ArrayList<>();

                for (Map<String, String> item : loadedPatternItems) {
                    String phrase = item.get("phrase");
                    String type = item.get("type");
                    if (phrase != null && !phrase.isEmpty()) {
                        allPatternStrings.add(phrase); // Add all phrases to allPatternStrings
                        if (type != null) {
                            if (type.startsWith("english_")) {
                                englishPhrases.add(phrase);
                            } else if (type.startsWith("japanese_")) {
                                japanesePhrases.add(phrase);
                            } else {
                                logger.warn("Pattern phrase '{}' has unrecognized type '{}', adding to generic English list for PromptInjectionRuleConfig.", phrase, type);
                                englishPhrases.add(phrase);
                            }
                        } else {
                           englishPhrases.add(phrase);
                           logger.warn("Pattern phrase '{}' has no type, defaulting to English for PromptInjectionRuleConfig.", phrase);
                        }
                    }
                }

                if (!englishPhrases.isEmpty()) {
                    this.englishInjectionPatterns = Map.of("general_english", englishPhrases);
                }
                if (!japanesePhrases.isEmpty()) {
                    this.japaneseInjectionPatterns = Map.of("general_japanese", japanesePhrases);
                }

                logger.info("Loaded {} English pattern phrases and {} Japanese pattern phrases into categories for PromptInjectionRuleConfig.",
                            englishPhrases.size(), japanesePhrases.size());
                logger.info("Total of {} pattern strings loaded for allPatternStrings in PromptInjectionRuleConfig.", allPatternStrings.size());

            } else if (rawPatterns != null) {
                 logger.error("YAML structure mismatch for 'injection_patterns'. Expected List<Map<String, String>> but found {}.", rawPatterns.getClass().getName());
            } else {
                logger.warn("No 'injection_patterns' found in rules file or it's not a list.");
            }

        } catch (ClassCastException cce) {
            // This explicit catch might be redundant if the instanceof check is robust, but kept for safety.
            logger.error("YAML structure mismatch during parsing. Error: {}", cce.getMessage(), cce);
        }
        catch (Exception e) {
            logger.error("Failed to load or parse prompt injection rules from: {}", rulesFile.getFilename(), e);
        }
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
