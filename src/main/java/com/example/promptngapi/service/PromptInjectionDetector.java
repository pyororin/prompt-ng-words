package com.example.promptngapi.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.io.InputStream;
import org.yaml.snakeyaml.Yaml;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 指定されたテキスト内の潜在的なプロンプトインジェクションの試みを検出するサービスです。
 * 既知のインジェクションフレーズのリストを使用し、それらの存在をチェックします。
 * ルールは `prompt_injection_rules.yaml` ファイルからロードされます。
 */
@Service
public class PromptInjectionDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptInjectionDetector.class);
    // For patterns that are actual regex
    private static final List<Pattern> REGEX_INJECTION_PATTERNS = new ArrayList<>();
    // For simple literal phrases (will be checked with contains, case-insensitively for English)
    private static final List<String> LITERAL_ENGLISH_PHRASES = new ArrayList<>();
    private static final List<String> LITERAL_JAPANESE_PHRASES = new ArrayList<>();
    private static final List<String> FORBIDDEN_WORDS_JP = new ArrayList<>();

    static {
        loadRulesFromYaml();
    }

    @SuppressWarnings("unchecked")
    private static void loadRulesFromYaml() {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = PromptInjectionDetector.class.getClassLoader().getResourceAsStream("prompt_injection_rules.yaml")) {
            if (inputStream == null) {
                LOGGER.error("Unable to find prompt_injection_rules.yaml. No rules will be loaded.");
                return;
            }
            Map<String, Object> data = yaml.load(inputStream);

            if (data == null) {
                LOGGER.error("prompt_injection_rules.yaml is empty or malformed. No rules will be loaded.");
                return;
            }

            // Load forbidden_words_jp
            List<String> loadedForbiddenWords = (List<String>) data.getOrDefault("forbidden_words_jp", new ArrayList<>());
            FORBIDDEN_WORDS_JP.addAll(loadedForbiddenWords);
            LOGGER.info("Loaded {} Japanese forbidden words.", FORBIDDEN_WORDS_JP.size());

            // Load injection_patterns
            List<Map<String, String>> loadedPatternMaps = (List<Map<String, String>>) data.get("injection_patterns");
            if (loadedPatternMaps != null) {
                int regexCount = 0;
                int literalEnCount = 0;
                int literalJaCount = 0;

                for (Map<String, String> patternMap : loadedPatternMaps) {
                    String phrase = patternMap.get("phrase");
                    String type = patternMap.get("type");

                    if (phrase == null || phrase.trim().isEmpty()) {
                        LOGGER.warn("Skipping empty or null phrase in injection_patterns.");
                        continue;
                    }

                    try {
                        if (type != null && type.endsWith("_regex")) {
                            REGEX_INJECTION_PATTERNS.add(Pattern.compile(phrase, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
                            regexCount++;
                        } else if (type != null && type.startsWith("english_")) {
                            LITERAL_ENGLISH_PHRASES.add(phrase.toLowerCase()); // Store in lowercase for case-insensitive contains
                            literalEnCount++;
                        } else if (type != null && type.startsWith("japanese_")) {
                            LITERAL_JAPANESE_PHRASES.add(phrase); // Store as is for Japanese
                            literalJaCount++;
                        } else { // Default to phrase matching (English-like case folding) if type is null or unrecognized
                            LITERAL_ENGLISH_PHRASES.add(phrase.toLowerCase());
                            literalEnCount++;
                             LOGGER.warn("Pattern phrase '{}' has no type or unrecognized type '{}', defaulting to English literal phrase.", phrase, type);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error processing pattern for phrase: '{}', type: '{}'. Error: {}", phrase, type, e.getMessage());
                    }
                }
                LOGGER.info("Loaded {} regex, {} English literal, {} Japanese literal injection patterns from YAML.", regexCount, literalEnCount, literalJaCount);
            } else {
                LOGGER.warn("No 'injection_patterns' section found in YAML or it's not a list of maps.");
            }

        } catch (Exception e) {
            LOGGER.error("Error loading or parsing prompt_injection_rules.yaml", e);
        }
    }


    // Old static block for hardcoded phrases - to be removed by diff
    /*
    static {
        // Initialize known prompt injection phrases.
        // "命令" means command/instruction
        // "無視" means ignore
        // "開示" means disclose
        // "漏洩" means leakage/disclosure
        // "実行" means execute/run
    );
    */


    /**
     * 指定されたテキストに既知のプロンプトインジェクションの試みが含まれているか、または禁止単語リストに一致する単語が含まれているかをチェックします。
     *
     * @param text チェックする入力テキスト。
     * @return 潜在的なプロンプトインジェクションの試みが見つかった場合は {@code true}、それ以外の場合は {@code false}。
     */
    public boolean isPromptInjectionAttempt(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String lowerCaseText = text.toLowerCase(); // For matching English literal phrases

        // Check literal English phrases (case-insensitive)
        for (String literalPhrase : LITERAL_ENGLISH_PHRASES) {
            if (lowerCaseText.contains(literalPhrase)) {
                return true;
            }
        }

        // Check literal Japanese phrases (case-sensitive, as toLowerCase() isn't effective for script variants)
        for (String literalPhrase : LITERAL_JAPANESE_PHRASES) {
            if (text.contains(literalPhrase)) {
                return true;
            }
        }

        // Check regex patterns
        for (Pattern regexPattern : REGEX_INJECTION_PATTERNS) {
            if (regexPattern.matcher(text).find()) {
                return true;
            }
        }

        // Check for individual forbidden words in Japanese
        if (containsForbiddenWordsJp(text)) {
            return true;
        }

        return false;
    }

    private boolean containsForbiddenWordsJp(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        // Simple contains check. For more accuracy, consider tokenization and normalization (e.g., Katakana to Hiragana).
        // Also, a substring match (e.g., "パス" in "パスポート") will trigger.
        // Current implementation does not normalize case (e.g. full-width vs half-width chars or Katakana vs Hiragana explicitly here).
        // The INJECTION_PATTERNS use UNICODE_CASE, but this is a simple string contains.
        // String lowerCaseText = text.toLowerCase(); // Removed as it's not very effective for Japanese script matching

        for (String forbiddenWord : FORBIDDEN_WORDS_JP) {
            // For Japanese, true script-variant insensitive matching would require more advanced normalization
            // or adding all variants to the forbidden list.
            // For now, direct substring check.
            if (text.contains(forbiddenWord)) {
                return true;
            }
        }
        return false;
    }
}
