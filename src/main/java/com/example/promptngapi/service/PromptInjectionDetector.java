package com.example.promptngapi.service;

import com.example.promptngapi.nlp.KuromojiAnalyzer; // KuromojiAnalyzerをインポート
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
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
import com.example.promptngapi.config.ScoreThresholdsConfig; // Added import
import java.util.regex.Matcher;

/**
 * 指定されたテキスト内の潜在的なプロンプトインジェクションの試みを検出するサービスです。
 * 既知のインジェクションフレーズのリストと、非日本語の文章の検出ロジックを使用します。
 * ルールは `prompt_injection_rules.yaml` ファイルからロードされます。
 */
@Service
public class PromptInjectionDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptInjectionDetector.class);
    // Apache Tika Optimaize言語検出器のインスタンス。言語モデルをロード済み。
    private static final LanguageDetector langDetector = new OptimaizeLangDetector().loadModels();
    private final KuromojiAnalyzer kuromojiAnalyzer; // KuromojiAnalyzerのインスタンス
    private final ScoreThresholdsConfig scoreThresholdsConfig; // Added field

    // 正規表現パターンを格納するリスト
    private static final List<Pattern> REGEX_PATTERNS = new ArrayList<>();
    // リテラルな英語フレーズ（toLowerCaseで格納し、大文字・小文字を区別せずにチェック）
    private static final List<String> LITERAL_ENGLISH_PHRASES = new ArrayList<>();
    // リテラルな日本語フレーズ
    private static final List<String> LITERAL_JAPANESE_PHRASES = new ArrayList<>();
    // 類似度チェック用のオリジナルフレーズ（正規表現ではないもの）を格納するリスト
    private static final List<String> ORIGINAL_PHRASES_FOR_SIMILARITY = new ArrayList<>();
    // 禁止されている日本語の単語リスト
    private static final List<String> FORBIDDEN_WORDS_JP = new ArrayList<>();

    // コンストラクタインジェクションを使用
    public PromptInjectionDetector(KuromojiAnalyzer kuromojiAnalyzer, ScoreThresholdsConfig scoreThresholdsConfig) {
        this.kuromojiAnalyzer = kuromojiAnalyzer;
        // ScoreThresholdsConfig をインジェクションして閾値を提供
        this.scoreThresholdsConfig = scoreThresholdsConfig;
        // loadRulesFromYaml(); // static初期化ブロックから移動させることを検討 (後述)
    }

    // loadRulesFromYaml は static メソッドなので、ここでは KuromojiAnalyzer を直接使えない。
    // ルールロード時にNLP処理が必要な場合は、このメソッドの呼び出し方や
    // ルール格納方法の変更が必要になるが、今回はまず判定時の利用に集中する。
    // もし loadRulesFromYaml 内で kuromojiAnalyzer を使いたい場合は、
    // このメソッドを非staticにするか、Analyzerを引数で渡す必要がある。
    // 今回の改修では、ルールのフレーズは判定時にその都度解析する方針とする。

    static {
        loadRulesForTesting(); // Call the new public method which includes clearing and loading
    }

    /**
     * Clears all rule lists and reloads them from the YAML file.
     * Intended for use in testing to ensure a clean rule state.
     */
    public static void loadRulesForTesting() {
        // Clear all static rule lists
        REGEX_PATTERNS.clear();
        LITERAL_ENGLISH_PHRASES.clear();
        LITERAL_JAPANESE_PHRASES.clear();
        ORIGINAL_PHRASES_FOR_SIMILARITY.clear();
        FORBIDDEN_WORDS_JP.clear();

        doLoadRulesFromYaml(); // Call the actual loading logic
    }

    @SuppressWarnings("unchecked")
    private static void doLoadRulesFromYaml() { // Renamed from loadRulesFromYaml
        Yaml yaml = new Yaml();
        try (InputStream inputStream = PromptInjectionDetector.class.getClassLoader().getResourceAsStream("prompt_injection_rules.yaml")) {
            if (inputStream == null) {
                LOGGER.error("prompt_injection_rules.yaml が見つかりません。ルールはロードされません。");
                return;
            }
            Map<String, Object> data = yaml.load(inputStream);

            if (data == null) {
                LOGGER.error("prompt_injection_rules.yaml が空または不正な形式です。ルールはロードされません。");
                return;
            }

            // forbidden_words_jp のロード
            List<String> loadedForbiddenWords = (List<String>) data.getOrDefault("forbidden_words_jp", new ArrayList<>());
            List<String> katakanaForbiddenWords = new ArrayList<>();
            for (String word : loadedForbiddenWords) {
                katakanaForbiddenWords.add(KuromojiAnalyzer.convertToKatakana(word));
            }
            FORBIDDEN_WORDS_JP.addAll(katakanaForbiddenWords);
            LOGGER.info("{}個の日本語禁止単語をロードし、カタカナに正規化して格納しました。", FORBIDDEN_WORDS_JP.size());

            // injection_patterns のロード
            List<Map<String, String>> loadedPatternMaps = (List<Map<String, String>>) data.get("injection_patterns");
            if (loadedPatternMaps != null) {
                int regexCount = 0;
                int literalEnCount = 0;
                int literalJaCount = 0;

                for (Map<String, String> patternMap : loadedPatternMaps) {
                    String phrase = patternMap.get("phrase");
                    String type = patternMap.get("type");

                    if (phrase == null || phrase.trim().isEmpty()) {
                        LOGGER.warn("injection_patterns内の空またはnullのフレーズをスキップします。");
                        continue;
                    }

                    try {
                        if (type != null && type.endsWith("_regex")) {
                            REGEX_PATTERNS.add(Pattern.compile(phrase, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
                            regexCount++;
                        } else { // リテラルなフレーズ
                            ORIGINAL_PHRASES_FOR_SIMILARITY.add(phrase);
                            if (type != null && type.startsWith("english_")) {
                                LITERAL_ENGLISH_PHRASES.add(phrase.toLowerCase());
                                literalEnCount++;
                            } else if (type != null && type.startsWith("japanese_")) {
                                LITERAL_JAPANESE_PHRASES.add(phrase);
                                literalJaCount++;
                            } else { // typeがnullまたは十分に具体的でない場合は、デフォルトで英語リテラルフレーズとして扱う
                                LITERAL_ENGLISH_PHRASES.add(phrase.toLowerCase());
                                literalEnCount++;
                                LOGGER.warn("パターンフレーズ '{}' にtypeがないか、認識できないtype '{}' です。完全一致のために英語リテラルフレーズとして扱います。", phrase, type);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("フレーズのパターン処理エラー: '{}', type: '{}'. エラー: {}", phrase, type, e.getMessage());
                    }
                }
                LOGGER.info("{}個の正規表現パターン、{}個の英語リテラルフレーズ、{}個の日本語リテラルフレーズをロードしました。類似度チェック用のフレーズは{}個です。",
                            regexCount, literalEnCount, literalJaCount, ORIGINAL_PHRASES_FOR_SIMILARITY.size());
            } else {
                LOGGER.warn("YAMLに 'injection_patterns' セクションが見つからないか、マップのリストではありません。");
            }

        } catch (Exception e) {
            LOGGER.error("prompt_injection_rules.yaml のロードまたは解析中にエラーが発生しました。", e);
        }
    }


    /**
     * 指定されたテキストに、既知のプロンプトインジェクションの試み、禁止単語、または非日本語の文章が含まれているかをチェックし、
     * 検出されたすべての問題の詳細リストを返します。
     *
     * @param text チェックする入力テキスト。
     * @return 検出されたすべてのインジェクション試みやその他の問題の詳細リスト。問題が見つからない場合は空のリスト。
     */
    public List<DetectionDetail> isPromptInjectionAttempt(String originalFullText) {
        List<DetectionDetail> allDetectedIssues = new ArrayList<>();
        if (originalFullText == null || originalFullText.isEmpty()) {
            return allDetectedIssues;
        }

        List<String> phrases = kuromojiAnalyzer.splitIntoPhrases(originalFullText);
        // LOGGER.info("Original text for phrase splitting: \"{}\"", originalFullText); // Temporary logging
        // LOGGER.info("Generated phrases: {}", phrases); // Temporary logging

        if (phrases.isEmpty() || phrases.stream().allMatch(String::isEmpty)) {
            LOGGER.warn("Text could not be split into phrases or resulted in empty phrases. Analyzing full text as a single phrase: {}", originalFullText);
            phrases = List.of(originalFullText);
        }

        for (String currentPhrase : phrases) {
            if (currentPhrase == null || currentPhrase.isEmpty()) {
                continue;
            }

            String lowerCasePhrase = currentPhrase.toLowerCase();
            String normalizedPhraseForForbiddenCheck = KuromojiAnalyzer.convertToKatakana(currentPhrase);
            List<String> analyzedPhraseTokens = kuromojiAnalyzer.analyzeText(currentPhrase);
            String analyzedPhraseForMatching = String.join(" ", analyzedPhraseTokens);

            // 1. 禁止されている日本語の単語をチェック
            for (String forbiddenWord : FORBIDDEN_WORDS_JP) {
                if (normalizedPhraseForForbiddenCheck.contains(forbiddenWord)) {
                    DetectionDetail newDetail = new DetectionDetail(
                        "prompt_injection_word_jp",
                        forbiddenWord,
                        currentPhrase,
                        1.0,
                        "禁止された日本語の単語が検出されました（カタカナ正規化後）。",
                        originalFullText
                    );
                    // LOGGER.info("Adding DetectionDetail: type={}, pattern={}, phrase=\"{}\"", newDetail.getType(), newDetail.getMatched_pattern(), newDetail.getInput_substring());
                    allDetectedIssues.add(newDetail);
                }
            }

            // 2. リテラルな英語フレーズをチェック
            for (String literalEngPhrase : LITERAL_ENGLISH_PHRASES) {
                if (lowerCasePhrase.contains(literalEngPhrase)) {
                    int startIndex = lowerCasePhrase.indexOf(literalEngPhrase);
                    String actualSubstringInPhrase = currentPhrase.substring(startIndex, startIndex + literalEngPhrase.length());
                    DetectionDetail newDetail = new DetectionDetail(
                        "prompt_injection_phrase_en",
                        literalEngPhrase,
                        actualSubstringInPhrase,
                        1.0,
                        "英語のフレーズ（大文字・小文字区別なし）に完全一致しました。",
                        originalFullText
                    );
                    // LOGGER.info("Adding DetectionDetail: type={}, pattern={}, phrase=\"{}\", matched_substring=\"{}\"", newDetail.getType(), newDetail.getMatched_pattern(), currentPhrase, actualSubstringInPhrase);
                    allDetectedIssues.add(newDetail);
                }
            }

            // 3. リテラルな日本語フレーズをチェック（NLPによる正規化と比較）
            if (!analyzedPhraseForMatching.isEmpty()) {
                for (String literalJpnPhrase : LITERAL_JAPANESE_PHRASES) {
                    List<String> analyzedRulePhraseTokens = kuromojiAnalyzer.analyzeText(literalJpnPhrase);
                    if (analyzedRulePhraseTokens.isEmpty()) {
                        continue;
                    }
                    String analyzedRulePhraseForMatching = String.join(" ", analyzedRulePhraseTokens);

                    if (analyzedPhraseForMatching.contains(analyzedRulePhraseForMatching)) {
                        DetectionDetail newDetail = new DetectionDetail(
                            "prompt_injection_phrase_ja_nlp",
                            literalJpnPhrase,
                            currentPhrase,
                            1.0,
                            "日本語のフレーズにNLP処理後の正規化文字列で一致しました。",
                            originalFullText
                        );
                        // LOGGER.info("Adding DetectionDetail: type={}, pattern=\"{}\", phrase=\"{}\"", newDetail.getType(), newDetail.getMatched_pattern(), newDetail.getInput_substring());
                        allDetectedIssues.add(newDetail);
                    }
                }
            }

            // 4. 正規表現パターンをチェック
            for (Pattern regexPattern : REGEX_PATTERNS) {
                Matcher matcher = regexPattern.matcher(currentPhrase);
                while (matcher.find()) {
                    DetectionDetail newDetail = new DetectionDetail(
                        "prompt_injection_regex",
                        regexPattern.pattern(),
                        matcher.group(),
                        1.0,
                        "正規表現パターンに一致しました。",
                        originalFullText
                    );
                    // LOGGER.info("Adding DetectionDetail: type={}, pattern=\"{}\", matched_in_phrase=\"{}\", phrase=\"{}\"", newDetail.getType(), newDetail.getMatched_pattern(), newDetail.getInput_substring(), currentPhrase);
                    allDetectedIssues.add(newDetail);
                }
            }

            // 5. オリジナルフレーズに対するJaro-Winkler類似度チェック (NLPで正規化後)
            if (!analyzedPhraseForMatching.isEmpty()) {
                JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();
                for (String originalRulePhraseForSimilarity : ORIGINAL_PHRASES_FOR_SIMILARITY) {
                    List<String> analyzedRulePhraseTokens = kuromojiAnalyzer.analyzeText(originalRulePhraseForSimilarity);
                    if (analyzedRulePhraseTokens.isEmpty()) {
                        continue;
                    }
                    String analyzedRulePhraseForSimilarity = String.join(" ", analyzedRulePhraseTokens);

                    double score = jaroWinkler.apply(analyzedPhraseForMatching, analyzedRulePhraseForSimilarity);

                    if (score >= scoreThresholdsConfig.getSimilarityThreshold()) {
                        boolean alreadyFoundExactOrNlpLiteral = false;
                        for (DetectionDetail detail : allDetectedIssues) {
                            if (detail.getInput_substring().equals(currentPhrase) && detail.getMatched_pattern().equalsIgnoreCase(originalRulePhraseForSimilarity)) {
                                if (detail.getType().equals("prompt_injection_phrase_en") ||
                                    detail.getType().equals("prompt_injection_phrase_ja_nlp") ||
                                    detail.getType().equals("prompt_injection_word_jp") ||
                                    detail.getType().equals("prompt_injection_regex")) {
                                    if (detail.getSimilarity_score() == null || detail.getSimilarity_score() == 1.0) {
                                        alreadyFoundExactOrNlpLiteral = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if (!alreadyFoundExactOrNlpLiteral) {
                            DetectionDetail newDetail = new DetectionDetail(
                                "prompt_injection_similarity_nlp",
                                originalRulePhraseForSimilarity,
                                currentPhrase,
                                score,
                                "既知のインジェクションフレーズとの類似度が高いです（NLP正規化後）。",
                                originalFullText
                            );
                            // LOGGER.info("Adding DetectionDetail (Similarity): type={}, pattern=\"{}\", phrase=\"{}\", score={}", newDetail.getType(), newDetail.getMatched_pattern(), newDetail.getInput_substring(), score);
                            allDetectedIssues.add(newDetail);
                        }
                    }
                }
            }

            // 6. 非日本語の文章をチェック (now applied per phrase)
            if (isNonJapaneseSentence(currentPhrase)) {
                DetectionDetail newDetail = new DetectionDetail(
                    "non_japanese_phrase",
                    currentPhrase,
                    currentPhrase,
                    1.0,
                    "入力フレーズに非日本語の文章が含まれています。",
                    originalFullText
                );
                // LOGGER.info("Adding DetectionDetail: type={}, phrase=\"{}\"", newDetail.getType(), newDetail.getInput_substring());
                allDetectedIssues.add(newDetail);
            }
        }
        // TODO: Consider more sophisticated duplicate/overlapping DetectionDetail filtering if needed.
        return allDetectedIssues;
    }

    // containsForbiddenWordsJp メソッドは isPromptInjectionAttempt に統合された

    /**
     * 指定されたテキストが非日本語の文章であるかどうかを判定します。
     * テキストの言語を検出し、それが日本語でない場合、かつ単語数が定義された閾値を超えている場合に true を返します。
     *
     * @param text チェックするテキスト。
     * @return テキストが非日本語の文章であれば true、そうでなければ false。
     */
    private boolean isNonJapaneseSentence(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false; // nullまたは空のテキストは対象外
        }

        LanguageResult result = langDetector.detect(text); // 言語を検出
        String language = result.getLanguage();

        // 言語が日本語でないかチェック
        if (!"ja".equals(language)) {
            // テキストを単語に分割
            int wordCount = text.trim().split("\\s+").length;
            // 設定ファイルから取得した非日本語文章の単語数閾値を使用
            // 単語数が閾値を超えていれば、非日本語の文章と判定
            return wordCount > scoreThresholdsConfig.getNonJapaneseSentenceWordThreshold();
        }
        // 言語が日本語であるか、または日本語でなくても単語数が閾値以下であれば false
        return false;
    }
}
