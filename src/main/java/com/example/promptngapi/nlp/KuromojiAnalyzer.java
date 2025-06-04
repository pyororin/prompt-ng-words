package com.example.promptngapi.nlp;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import org.springframework.stereotype.Component; // Componentをインポート

import java.util.List;
import java.util.stream.Collectors;

@Component // Springが管理するBeanとしてマーク
public class KuromojiAnalyzer {

    private static final Tokenizer tokenizer = new Tokenizer();

    /**
     * 指定されたテキストを形態素解析し、トークンのリストを返します。
     *
     * @param text 解析するテキスト
     * @return トークンのリスト
     */
    public List<Token> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return List.of(); // 空のリストを返す
        }
        return tokenizer.tokenize(text);
    }

    /**
     * 指定されたテキストを形態素解析し、フィルタリングと正規化を行った単語のリストを返します。
     * - 動詞と形容詞は原形に変換されます。
     * - 助詞、助動詞、記号、空白は除外されます。
     *
     * @param text 解析するテキスト
     * @return 処理済みの単語リスト
     */
    public List<String> analyzeText(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<Token> tokens = tokenize(text);
        return tokens.stream()
                .filter(token -> {
                    String partOfSpeech = token.getPartOfSpeechLevel1();
                    // 除外する品詞: 助詞, 助動詞, 記号, 空白など (必要に応じて調整)
                    // 接頭詞、接続詞、連体詞、フィラーなども除外対象として検討可能
                    return !partOfSpeech.equals("助詞") &&
                           !partOfSpeech.equals("助動詞") &&
                           !partOfSpeech.equals("記号") &&
                           !partOfSpeech.equals("空白") &&
                           !partOfSpeech.equals("その他") && // その他（間投詞など）も除外することが多い
                           !token.getSurface().trim().isEmpty();
                })
                .map(token -> {
                    String partOfSpeech = token.getPartOfSpeechLevel1();
                    String baseForm = token.getBaseForm();
                    String surface = token.getSurface();
                    String reading = token.getReading();

                    String chosenText;

                    // Normalization Logic
                    // 1. If the surface form is already fully Katakana, use it.
                    if (isKatakana(surface)) {
                        chosenText = surface;
                    // 2. Else, if the reading is available and fully Katakana, use the reading.
                    } else if (reading != null && !reading.isEmpty() && isKatakana(reading)) {
                        chosenText = reading;
                    // 3. Else, for verbs and adjectives, use the base form if available.
                    } else if (("動詞".equals(partOfSpeech) || "形容詞".equals(partOfSpeech)) && baseForm != null && !baseForm.equals("*")) {
                        chosenText = baseForm;
                    // 4. Otherwise, use the surface form.
                    } else {
                        chosenText = surface;
                    }

                    // Convert the final chosen text to Katakana (this handles Hiragana in baseForm/surface if selected)
                    return convertToKatakana(chosenText);
                })
                .collect(Collectors.toList());
    }

    public static boolean isKatakana(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (char c : text.toCharArray()) {
            // Check if the character is Katakana (Unicode range U+30A0 to U+30FF)
            // Includes ゠ (U+30A0) to ヿ (U+30FF)
            // Excludes half-width Katakana (U+FF65 to U+FF9F)
            if (!(c >= '\u30A0' && c <= '\u30FF')) {
                return false;
            }
        }
        return true;
    }

    /**
     * 主にテストやデバッグのために、トークンの詳細情報を文字列として出力します。
     * @param text 解析するテキスト
     * @return トークン情報の文字列
     */
    public String getTokensDetails(String text) {
        if (text == null || text.isEmpty()) {
            return "Input text is empty.";
        }
        List<Token> tokens = tokenize(text);
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens) {
            sb.append("Surface: ").append(token.getSurface())
              .append(", BaseForm: ").append(token.getBaseForm())
              .append(", PartOfSpeech: ").append(token.getPartOfSpeechLevel1())
              .append("-").append(token.getPartOfSpeechLevel2())
              .append("-").append(token.getPartOfSpeechLevel3())
              .append("-").append(token.getPartOfSpeechLevel4())
              .append(", Reading: ").append(token.getReading())
              .append(", Pronunciation: ").append(token.getPronunciation())
              .append("\n");
        }
        return sb.toString();
    }

    // 必要に応じて、同義語処理やさらなる正規化処理メソッドをここに追加できます。

    public static String convertToKatakana(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            // Check if the character is Hiragana (Unicode range U+3040 to U+309F)
            // Includes ぀ (U+3040) to ゟ (U+309F)
            if (c >= '\u3040' && c <= '\u309F') {
                // Convert Hiragana to Katakana by adding 0x60
                sb.append((char) (c + 0x60));
            } else {
                // Keep other characters (Katakana, Kanji, alphanumeric, symbols) as they are
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
