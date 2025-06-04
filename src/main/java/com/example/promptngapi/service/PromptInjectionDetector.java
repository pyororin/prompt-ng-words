package com.example.promptngapi.service;

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
import java.util.regex.Matcher;

/**
 * 指定されたテキスト内の潜在的なプロンプトインジェクションの試みを検出するサービスです。
 * 既知のインジェクションフレーズのリストと、非日本語の文章の検出ロジックを使用します。
 * ルールは `prompt_injection_rules.yaml` ファイルからロードされます。
 */
@Service
public class PromptInjectionDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptInjectionDetector.class);
    private static final double SIMILARITY_THRESHOLD = 0.7; // Jaro-Winkler類似度チェックの閾値
    // 非日本語の文章と判定するための単語数の閾値。この値より多い単語数を持つ非日本語の文章が検出対象。
    private static final int NON_JAPANESE_SENTENCE_WORD_THRESHOLD = 3;
    // Apache Tika Optimaize言語検出器のインスタンス。言語モデルをロード済み。
    private static final LanguageDetector langDetector = new OptimaizeLangDetector().loadModels();

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

    static {
        loadRulesFromYaml();
    }

    @SuppressWarnings("unchecked")
    private static void loadRulesFromYaml() {
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
            FORBIDDEN_WORDS_JP.addAll(loadedForbiddenWords);
            LOGGER.info("{}個の日本語禁止単語をロードしました。", FORBIDDEN_WORDS_JP.size());

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


    // ハードコードされたフレーズ用の古い静的ブロック - diffによって削除される予定
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
     * 指定されたテキストに、既知のプロンプトインジェクションの試み、禁止単語、または非日本語の文章が含まれているかをチェックし、
     * 検出されたすべての問題の詳細リストを返します。
     *
     * @param text チェックする入力テキスト。
     * @return 検出されたすべてのインジェクション試みやその他の問題の詳細リスト。問題が見つからない場合は空のリスト。
     */
    public List<DetectionDetail> isPromptInjectionAttempt(String text) {
        List<DetectionDetail> detectedIssues = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return detectedIssues;
        }

        String lowerCaseText = text.toLowerCase(); // 英語リテラルフレーズのマッチング用

        // 1. 禁止されている日本語の単語をチェック
        for (String forbiddenWord : FORBIDDEN_WORDS_JP) {
            if (text.contains(forbiddenWord)) { // 日本語は元のテキストでチェック
                detectedIssues.add(new DetectionDetail(
                    "prompt_injection_word_jp",
                    forbiddenWord,
                    forbiddenWord, // 単語リストの場合、マッチしたパターンと部分文字列は単語自体
                    1.0,
                    "禁止された日本語の単語が検出されました。"
                ));
            }
        }

        // 2. リテラルな英語フレーズをチェック（大文字・小文字を区別しない部分文字列の一致）
        for (String literalEngPhrase : LITERAL_ENGLISH_PHRASES) {
            // literalEngPhraseは既に小文字で格納されている
            if (lowerCaseText.contains(literalEngPhrase)) {
                // より正確な報告のために、元のテキストで実際の部分文字列を見つける
                int startIndex = lowerCaseText.indexOf(literalEngPhrase);
                String actualSubstring = text.substring(startIndex, startIndex + literalEngPhrase.length());
                detectedIssues.add(new DetectionDetail(
                    "prompt_injection_phrase_en",
                    literalEngPhrase, // パターン（小文字）を格納
                    actualSubstring,  // 元の大文字・小文字を含む部分文字列を格納
                    1.0,
                    "英語のフレーズ（大文字・小文字区別なし）に完全一致しました。"
                ));
            }
        }

        // 3. リテラルな日本語フレーズをチェック（大文字・小文字を区別する部分文字列の一致）
        for (String literalJpnPhrase : LITERAL_JAPANESE_PHRASES) {
            if (text.contains(literalJpnPhrase)) {
                detectedIssues.add(new DetectionDetail(
                    "prompt_injection_phrase_ja",
                    literalJpnPhrase,
                    literalJpnPhrase, // 部分文字列はフレーズ自体
                    1.0,
                    "日本語のフレーズに完全一致しました。"
                ));
            }
        }

        // 4. 正規表現パターンをチェック
        for (Pattern regexPattern : REGEX_PATTERNS) {
            Matcher matcher = regexPattern.matcher(text);
            while (matcher.find()) { // 正規表現がアンカーされていない場合、すべての一致を見つけるためにwhileを使用
                detectedIssues.add(new DetectionDetail(
                    "prompt_injection_regex",
                    regexPattern.pattern(),
                    matcher.group(),
                    1.0, // 正規表現のマッチはこの目的では完全一致と見なされる
                    "正規表現パターンに一致しました。"
                ));
            }
        }

        // 5. オリジナルフレーズに対するJaro-Winkler類似度チェック
        // これは最後に行われ、注意深く扱わないと既存の検出に追加される可能性がある。
        // 現時点では、見逃しを防ぐためにスコアが高い場合に追加する。
        // この相互作用の重複排除や改良は将来のステップとなる可能性がある。
        JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();
        for (String originalPhrase : ORIGINAL_PHRASES_FOR_SIMILARITY) {
            double score = jaroWinkler.apply(text, originalPhrase); // 完全な入力テキストとオリジナルフレーズを比較
            if (score >= SIMILARITY_THRESHOLD) {
                // この *同じオリジナルフレーズ* がリテラルチェックで既に完全一致として見つかっている場合は追加を避ける
                boolean alreadyFoundExact = false;
                for (DetectionDetail detail : detectedIssues) {
                    if (detail.getMatched_pattern().equalsIgnoreCase(originalPhrase) && detail.getSimilarity_score() != null && detail.getSimilarity_score() == 1.0) {
                        alreadyFoundExact = true;
                        break;
                    }
                }
                if (!alreadyFoundExact) {
                     // 類似度の場合、input_substringは全文、またはスニペットになる可能性がある。
                     // 簡単のため、現時点ではオリジナルフレーズを「見つかった」部分文字列として使用する。
                    detectedIssues.add(new DetectionDetail(
                        "prompt_injection_similarity",
                        originalPhrase,
                        text, // またはテキストが長すぎる場合は関連するスニペットを見つける
                        score,
                        "既知のインジェクションフレーズとの類似度が高いです。"
                    ));
                }
            }
        }

        // 6. 非日本語の文章をチェック
        if (isNonJapaneseSentence(text)) {
            detectedIssues.add(new DetectionDetail(
                "non_japanese_sentence",
                text, // 現時点ではマッチしたパターンとして全文を使用
                text, // 現時点では入力部分文字列として全文を使用
                1.0,  // 明確な検出
                "入力に非日本語の文章が含まれています。"
            ));
        }
        // TODO: 複数のパターン（例：完全一致と類似度）が同じ入力部分にマッチする場合の重複排除ロジックを検討する。
        return detectedIssues;
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
            // 単語数が閾値を超えていれば、非日本語の文章と判定
            return wordCount > NON_JAPANESE_SENTENCE_WORD_THRESHOLD;
        }
        // 言語が日本語であるか、または日本語でなくても単語数が閾値以下であれば false
        return false;
    }
}
