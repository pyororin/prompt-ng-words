package com.example.promptngapi.service;

import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer;
import org.apache.lucene.analysis.ja.tokenattributes.BaseFormAttribute;
import org.apache.lucene.analysis.ja.tokenattributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.TokenStream;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import java.io.IOException;

import org.springframework.stereotype.Service;

@Service
public class NlpSimilarityService {

    private final JapaneseAnalyzer analyzer;

    public NlpSimilarityService() {
        this.analyzer = new JapaneseAnalyzer();
    }

    /**
     * Tokenizes the given Japanese text into a set of base form words,
     * filtering out particles and auxiliary verbs.
     *
     * @param text The Japanese text to tokenize.
     * @return A set of tokenized base forms.
     * @throws IOException If an error occurs during tokenization.
     */
    private Set<String> tokenize(String text) throws IOException {
        Set<String> tokens = new HashSet<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }

        try (TokenStream tokenStream = analyzer.tokenStream("field", new StringReader(text))) {
            CharTermAttribute charTermAttr = tokenStream.addAttribute(CharTermAttribute.class);
            BaseFormAttribute baseFormAttr = tokenStream.addAttribute(BaseFormAttribute.class);
            PartOfSpeechAttribute posAttr = tokenStream.addAttribute(PartOfSpeechAttribute.class);

            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                String term = charTermAttr.toString();
                String baseForm = baseFormAttr.getBaseForm();
                String partOfSpeech = posAttr.getPartOfSpeech();

                // Use base form if available, otherwise use the term itself
                String tokenToAdd = (baseForm != null) ? baseForm : term;

                // Filter out particles, auxiliary verbs, symbols, and punctuation
                // This is a basic filter and can be expanded.
                if (partOfSpeech != null &&
                    !(partOfSpeech.startsWith("助詞") || // Particle
                      partOfSpeech.startsWith("助動詞") || // Auxiliary verb
                      partOfSpeech.startsWith("記号") ||   // Symbol
                      partOfSpeech.equals("補助記号"))) { // Punctuation
                    tokens.add(tokenToAdd);
                } else if (partOfSpeech == null) {
                    // If part of speech is null, decide whether to add the token or not.
                    // For now, we'll add it if it's not empty.
                    if (!tokenToAdd.trim().isEmpty()){
                        tokens.add(tokenToAdd);
                    }
                }
            }
            tokenStream.end();
        }
        return tokens;
    }

    /**
     * Calculates the Jaccard similarity between two texts based on their token sets.
     *
     * @param text1 The first text.
     * @param text2 The second text.
     * @return The Jaccard similarity score (0.0 to 1.0).
     * @throws IOException If an error occurs during tokenization.
     */
    public double calculateSimilarity(String text1, String text2) throws IOException {
        if (text1 == null || text2 == null) {
            return 0.0;
        }
        if (text1.equals(text2)) {
            return 1.0;
        }

        Set<String> tokens1 = tokenize(text1);
        Set<String> tokens2 = tokenize(text2);

        if (tokens1.isEmpty() && tokens2.isEmpty()) {
            return 1.0; // Both empty, considered perfectly similar
        }
        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return 0.0; // One empty, one not, no similarity
        }

        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);

        Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);

        if (union.isEmpty()) {
            return 1.0; // Should not happen if tokens1 or tokens2 are not empty, but as a safe guard.
        }

        return (double) intersection.size() / union.size();
    }
}
