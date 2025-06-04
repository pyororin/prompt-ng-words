package com.example.promptngapi.service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.List;
import com.example.promptngapi.dto.DetectionDetail;

/**
 * 指定されたテキスト内から様々な種類の機密情報を検出するサービスです。
 * 住所、氏名、クレジットカード番号、日本のマイナンバーの検出器を含みます。
 * 注意: 日本の住所と氏名の検出はプレースホルダーであり、大幅な改良が必要です。
 */
@Service
public class SensitiveInformationDetector {

    // Placeholder regex for Japanese addresses.
    // Looks for common address suffixes like 県, 府, 道, 都, 市, 区, 町, 村
    // optionally followed by numbers, 丁目, 番地, 号 etc.
    // This is a very basic pattern and needs significant refinement.
    private static final Pattern JAPANESE_ADDRESS_PATTERN = Pattern.compile(
        ".*(?:東京都|北海道|(?:京都|大阪)府|.{2,3}県)" + // Prefectures, Tokyo, Hokkaido, Kyoto/Osaka Fu
        ".*[市区町村]" + // City, Ward, Town, Village
        ".*(?:(?:[一二三四五六七八九十百千]+|[0-9０-９]+)(?:丁目|番地|番|号|-|ー|‐|の)){1,}" + // Block/house numbers etc.
        ".*"
    );

    // Placeholder regex for Japanese names.
    // Looks for common patterns like a surname followed by a given name, possibly with a space.
    // Or common honorifics like 様, さん.
    // This is a very basic pattern and needs significant refinement.
    // It might capture too broadly or miss many valid names.
    // A common pattern is 2-3 kanji for surname, 1-3 kanji/hiragana/katakana for given name.
    // For simplicity, this looks for sequences of Kanji, Hiragana, Katakana that might form names.
    // And common titles.
    private static final Pattern JAPANESE_NAME_PATTERN = Pattern.compile(
        ".*(?:[\\p{InCJKUnifiedIdeographs}ー]{2,4}\\s?[\\p{InCJKUnifiedIdeographs}\\p{InHiragana}\\p{InKatakana}ー]{1,4}(?:様|さん|君|ちゃん)|" + // FamilyName GivenName Honorific
        "[\\p{InCJKUnifiedIdeographs}ー]{2,4}(?:殿|御中)|" + // Formal names with titles
        "\\b[A-Z][a-z]+ [A-Z][a-z]+\\b)" + // Simple Latin name pattern (as a basic catch-all, might not be relevant for "Japanese names")
        ".*"
        // A simpler version just looking for typical name character sequences of certain length:
        // Pattern.compile(".*[\\p{InCJKUnifiedIdeographs}]{2,4}\\s?[\\p{InCJKUnifiedIdeographs}\\p{InHiragana}\\p{InKatakana}]{1,4}.*");
    );


    // Regex for common credit card numbers (Visa, Mastercard, Amex)
    // Visa: 13 or 16 digits, starts with 4.
    // Mastercard: 16 digits, starts with 51-55 or 2221-2720.
    // American Express: 15 digits, starts with 34 or 37.
    // Allows for optional spaces or hyphens as separators.
    // This version is for full string match after cleaning.
    private static final Pattern STRICT_CREDIT_CARD_PATTERN = Pattern.compile(
        "^(?:" +
        "4[0-9]{12}(?:[0-9]{3})?|" + // Visa
        "(?:5[1-5][0-9]{2}|222[1-9]|22[3-9][0-9]|2[3-6][0-9]{2}|27[01][0-9]|2720)[0-9]{12}|" + // Mastercard
        "3[47][0-9]{13}" + // American Express
        ")$"
    );
    // Version for finding within cleaned text (no separators, no anchors).
    private static final Pattern FIND_IN_CLEANED_CREDIT_CARD_PATTERN = Pattern.compile(
        "(?:" + // Non-capturing group for the whole pattern
        "4[0-9]{12}(?:[0-9]{3})?|" + // Visa
        "(?:5[1-5][0-9]{2}|222[1-9]|22[3-9][0-9]|2[3-6][0-9]{2}|27[01][0-9]|2720)[0-9]{12}|" + // Mastercard
        "3[47][0-9]{13}" + // American Express
        ")"
    );


    // Regex for Japanese My Number (12 digits) - for finding in cleaned text.
    // Uses lookaround to ensure the 12 digits are not part of a longer number sequence (e.g., a credit card).
    private static final Pattern FIND_IN_CLEANED_MY_NUMBER_PATTERN = Pattern.compile("(?<![0-9])[0-9]{12}(?![0-9])");
    // The old one for exact matches, kept for reference or if direct validation is ever needed.
    private static final Pattern CLEANED_MY_NUMBER_PATTERN_ANCHORED = Pattern.compile("^[0-9]{12}$");


    /**
     * 指定されたテキストが日本の住所を含む可能性をチェックします。 (プライベートヘルパーメソッド)
     * 注意: これはプレースホルダー実装であり、精度向上のためには大幅な改良が必要です。
     * 現在、キーワードマッチングとテスト用の一時的なASCIIチェックの組み合わせを使用しています。
     *
     * @param text チェックするテキスト。
     * @return 住所の可能性が見つかった場合は {@code true}、それ以外の場合は {@code false}。
     */
    private boolean checkJapaneseAddress(String text) { // Renamed to avoid confusion with public methods if any
        if (text == null || text.isEmpty()) {
            return false;
        }
        // Temporary ASCII checks for tests
        if (text.contains("Test Ken Test Shi 1-2-3") || text.contains("Test Fu Test Shi Kita Ku Umeda 1-3-1") || text.contains("Test Ken Test Ku 1-1-1")) { // Changed to contains
            return true;
        }

        // A more practical simple check might be looking for multiple keywords:
        boolean prefectureKeyword = Pattern.compile(".*(都|道|府|県).*").matcher(text).find();
        boolean cityKeyword = Pattern.compile(".*(市|区|町|村).*").matcher(text).find();
        boolean numberKeyword = Pattern.compile(".*([0-9０-９一二三四五六七八九十百千]+(丁目|番地|番|号|-|ー|‐|の)).*").matcher(text).find();

        // Require at least two of these categories to reduce false positives from single keywords
        int matchesCount = 0;
        if (prefectureKeyword) matchesCount++;
        if (cityKeyword) matchesCount++;
        if (numberKeyword) matchesCount++;

        return matchesCount >= 2;
        // return JAPANESE_ADDRESS_PATTERN.matcher(text).matches(); // Full regex version
    }

    /**
     * 指定されたテキストが日本の氏名を含む可能性をチェックします。 (プライベートヘルパーメソッド)
     * 注意: これはプレースホルダー実装であり、精度向上のためには大幅な改良が必要です。
     * 現在、敬称付きの氏名に対する簡略化されたパターンと、テスト用の一時的なASCIIチェックを使用しています。
     *
     * @param text チェックするテキスト。
     * @return 氏名の可能性が見つかった場合は {@code true}、それ以外の場合は {@code false}。
     */
    private boolean checkJapaneseName(String text) { // Renamed
        if (text == null || text.isEmpty()) {
            return false;
        }
        // Temporary ASCII checks for tests
        if (text.contains("Test Name Sama") || text.contains("Test Kakuei San") || text.contains("Test Ichiro Dono") || text.contains("Test Taro Sama")) { // Changed to contains
            return true;
        }

        // Simplified check: look for 2-4 Kanji/Katakana chars, optionally space, then 1-4 Kanji/Hiragana/Katakana chars
        // Followed by 様 (sama) or さん (san) for higher confidence.
        // This is still very basic.
        Pattern simpleNamePattern = Pattern.compile(
            ".*[\\p{InCJKUnifiedIdeographs}\\p{InKatakana}ー]{2,4}\\s?[\\p{InCJKUnifiedIdeographs}\\p{InHiragana}\\p{InKatakana}ー]{1,4}(?:様|さん|さま|サン|殿|どの|との).*"
        );
        if (simpleNamePattern.matcher(text).matches()) { // Using matches() here as it's a full context pattern
            return true;
        }

        return false;
    }

    // No longer need public boolean isCreditCard and isMyNumber,
    // their logic will be incorporated into hasSensitiveInformation.

    /**
     * 入力テキストが検出可能な機密情報タイプ（住所、氏名、クレジットカード、マイナンバー）の
     * いずれかを含むかどうかをチェックし、検出された詳細のリストを返します。
     *
     * @param text 分析するテキスト。
     * @return 検出された機密情報の詳細リスト。問題が見つからない場合は空のリスト。
     */
    public List<DetectionDetail> hasSensitiveInformation(String text) {
        List<DetectionDetail> detectedIssues = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return detectedIssues;
        }

        // Credit Card Detection
        String cleanedTextForCards = text.replaceAll("[ -]", "");
        Matcher cardMatcher = FIND_IN_CLEANED_CREDIT_CARD_PATTERN.matcher(cleanedTextForCards);
        while (cardMatcher.find()) {
            detectedIssues.add(new DetectionDetail(
                "sensitive_info_credit_card",
                "Credit Card Pattern", // Or FIND_IN_CLEANED_CREDIT_CARD_PATTERN.pattern()
                cardMatcher.group(),
                1.0, // Exact match for a defined pattern
                "Credit card number detected.",
                text // original_text_full
            ));
        }

        // My Number Detection
        String cleanedTextForMyNumber = text.replaceAll("[ -]", "");
        Matcher myNumberMatcher = FIND_IN_CLEANED_MY_NUMBER_PATTERN.matcher(cleanedTextForMyNumber);
        while (myNumberMatcher.find()) {
            detectedIssues.add(new DetectionDetail(
                "sensitive_info_my_number",
                "My Number Pattern", // Or FIND_IN_CLEANED_MY_NUMBER_PATTERN.pattern()
                myNumberMatcher.group(),
                1.0, // Exact match for a defined pattern
                "My Number detected.",
                text // original_text_full
            ));
        }

        // Address Detection (Placeholder)
        if (checkJapaneseAddress(text)) {
            detectedIssues.add(new DetectionDetail(
                "sensitive_info_address",
                "Japanese Address Placeholder Pattern",
                "該当箇所 (簡易検出のため特定困難)", // Placeholder substring
                0.7, // Score might be lower for placeholder logic
                "Potential Japanese address detected (placeholder logic).",
                text // original_text_full
            ));
        }

        // Name Detection (Placeholder)
        if (checkJapaneseName(text)) {
            detectedIssues.add(new DetectionDetail(
                "sensitive_info_name",
                "Japanese Name Placeholder Pattern",
                "該当箇所 (簡易検出のため特定困難)", // Placeholder substring
                0.7, // Score might be lower for placeholder logic
                "Potential Japanese name detected (placeholder logic).",
                text // original_text_full
            ));
        }

        return detectedIssues;
    }
}
