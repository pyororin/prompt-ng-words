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
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import com.example.promptngapi.dto.DetectionDetail;
import java.util.regex.Matcher;

/**
 * 指定されたテキスト内の潜在的なプロンプトインジェクションの試みを検出するサービスです。
 * 既知のインジェクションフレーズのリストを使用し、それらの存在をチェックします。
 * ルールは `prompt_injection_rules.yaml` ファイルからロードされます。
 */
@Service
public class PromptInjectionDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptInjectionDetector.class);
    private static final double SIMILARITY_THRESHOLD = 0.85;

    // For patterns that are actual regex
    private static final List<Pattern> REGEX_PATTERNS = new ArrayList<>();
    // For simple literal phrases (will be checked with contains, case-insensitively for English)
    private static final List<String> LITERAL_ENGLISH_PHRASES = new ArrayList<>();
    private static final List<String> LITERAL_JAPANESE_PHRASES = new ArrayList<>();
    // Stores original phrases for similarity checks if they are not regex
    private static final List<String> ORIGINAL_PHRASES_FOR_SIMILARITY = new ArrayList<>();
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
                            REGEX_PATTERNS.add(Pattern.compile(phrase, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
                            regexCount++;
                        } else { // Literal phrases
                            ORIGINAL_PHRASES_FOR_SIMILARITY.add(phrase);
                            if (type != null && type.startsWith("english_")) {
                                LITERAL_ENGLISH_PHRASES.add(phrase.toLowerCase());
                                literalEnCount++;
                            } else if (type != null && type.startsWith("japanese_")) {
                                LITERAL_JAPANESE_PHRASES.add(phrase);
                                literalJaCount++;
                            } else { // Default to English literal phrase if type is null or not specific enough
                                LITERAL_ENGLISH_PHRASES.add(phrase.toLowerCase());
                                literalEnCount++;
                                LOGGER.warn("Pattern phrase '{}' has no type or unrecognized type '{}', defaulting to English literal phrase for exact matching.", phrase, type);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error processing pattern for phrase: '{}', type: '{}'. Error: {}", phrase, type, e.getMessage());
                    }
                }
                LOGGER.info("Loaded {} regex patterns, {} English literal phrases, {} Japanese literal phrases. {} phrases for similarity check.",
                            regexCount, literalEnCount, literalJaCount, ORIGINAL_PHRASES_FOR_SIMILARITY.size());
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
     * 指定されたテキストに既知のプロンプトインジェクションの試みが含まれているか、または禁止単語リストに一致する単語が含まれているかをチェックし、
     * 検出されたすべての詳細リストを返します。
     *
     * @param text チェックする入力テキスト。
     * @return 検出されたすべてのインジェクション試みの詳細リスト。問題が見つからない場合は空のリスト。
     */
    public List<DetectionDetail> isPromptInjectionAttempt(String text) {
        List<DetectionDetail> detectedIssues = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return detectedIssues;
        }

        String lowerCaseText = text.toLowerCase(); // For matching English literal phrases

        // 1. Check Forbidden Japanese Words
        for (String forbiddenWord : FORBIDDEN_WORDS_JP) {
            if (text.contains(forbiddenWord)) { // Using original text for Japanese
                detectedIssues.add(new DetectionDetail(
                    "prompt_injection_word_jp",
                    forbiddenWord,
                    forbiddenWord, // For word list, matched pattern and substring are the word itself
                    1.0,
                    "Forbidden Japanese word detected."
                ));
            }
        }

        // 2. Check Literal English Phrases (Exact Substring, Case-Insensitive)
        for (String literalEngPhrase : LITERAL_ENGLISH_PHRASES) {
            // literalEngPhrase is already stored in lowercase
            if (lowerCaseText.contains(literalEngPhrase)) {
                // Find the actual substring in the original text for more accurate reporting
                int startIndex = lowerCaseText.indexOf(literalEngPhrase);
                String actualSubstring = text.substring(startIndex, startIndex + literalEngPhrase.length());
                detectedIssues.add(new DetectionDetail(
                    "prompt_injection_phrase_en",
                    literalEngPhrase, // Store the pattern (lowercase)
                    actualSubstring,  // Store the original cased substring
                    1.0,
                    "Exact English phrase match (case-insensitive)."
                ));
            }
        }

        // 3. Check Literal Japanese Phrases (Exact Substring, Case-Sensitive)
        for (String literalJpnPhrase : LITERAL_JAPANESE_PHRASES) {
            if (text.contains(literalJpnPhrase)) {
                detectedIssues.add(new DetectionDetail(
                    "prompt_injection_phrase_ja",
                    literalJpnPhrase,
                    literalJpnPhrase, // Substring is the phrase itself
                    1.0,
                    "Exact Japanese phrase match."
                ));
            }
        }

        // 4. Check Regex Patterns
        for (Pattern regexPattern : REGEX_PATTERNS) {
            Matcher matcher = regexPattern.matcher(text);
            while (matcher.find()) { // Use while to find all occurrences if regex is not anchored
                detectedIssues.add(new DetectionDetail(
                    "prompt_injection_regex",
                    regexPattern.pattern(),
                    matcher.group(),
                    1.0, // Regex matches are considered exact for this purpose
                    "Regex pattern matched."
                ));
            }
        }

        // 5. Jaro-Winkler Similarity Check for original phrases
        // This is done last and could potentially add to existing detections if not handled carefully.
        // For now, we will add if score is high, focusing on not missing things.
        // Deduplication or refining this interaction can be a future step.
        JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();
        for (String originalPhrase : ORIGINAL_PHRASES_FOR_SIMILARITY) {
            double score = jaroWinkler.apply(text, originalPhrase); // Compare full input text with original phrase
            if (score >= SIMILARITY_THRESHOLD) {
                // Avoid adding if an exact match for this *same originalPhrase* was already found by literal checks
                boolean alreadyFoundExact = false;
                for (DetectionDetail detail : detectedIssues) {
                    if (detail.getMatched_pattern().equalsIgnoreCase(originalPhrase) && detail.getSimilarity_score() != null && detail.getSimilarity_score() == 1.0) {
                        alreadyFoundExact = true;
                        break;
                    }
                }
                if (!alreadyFoundExact) {
                     // For similarity, input_substring could be the whole text, or a snippet.
                     // For simplicity, using the original phrase as the "found" substring for now.
                    detectedIssues.add(new DetectionDetail(
                        "prompt_injection_similarity",
                        originalPhrase,
                        text, // Or find a relevant snippet if text is too long
                        score,
                        "High similarity to known injection phrase."
                    ));
                }
            }
        }
        // TODO: Consider deduplication logic here if multiple patterns (e.g., exact and similarity) match the same input part.
        return detectedIssues;
    }

    // containsForbiddenWordsJp method is now integrated into isPromptInjectionAttempt
}
