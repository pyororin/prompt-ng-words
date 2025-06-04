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
                    String baseForm = token.getBaseForm(); // 原形（活用しない語ではnullになることがある）
                    String surface = token.getSurface();   // 表層形

                    // 動詞と形容詞は原形を使用する
                    if ("動詞".equals(partOfSpeech) || "形容詞".equals(partOfSpeech)) {
                        return (baseForm != null && !baseForm.equals("*")) ? baseForm : surface;
                    }
                    // 名詞などはそのまま表層形を使用する（必要に応じて正規化処理を追加）
                    // 例えば、名詞の異表記正規化（例：「コンピュータ」と「コンピューター」）は別途辞書やロジックが必要
                    return surface;
                })
                .collect(Collectors.toList());
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
}
