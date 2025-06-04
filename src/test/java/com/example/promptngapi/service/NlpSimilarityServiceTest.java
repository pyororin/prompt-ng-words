package com.example.promptngapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

public class NlpSimilarityServiceTest {

    private NlpSimilarityService nlpSimilarityService;

    @BeforeEach
    void setUp() {
        nlpSimilarityService = new NlpSimilarityService();
    }

    @Test
    void testIdenticalTexts() throws IOException {
        String text1 = "これはテストです";
        String text2 = "これはテストです";
        assertEquals(1.0, nlpSimilarityService.calculateSimilarity(text1, text2), "Identical texts should have similarity 1.0");
    }

    @Test
    void testCompletelyDifferentTexts() throws IOException {
        String text1 = "これはテストです";
        String text2 = "全く異なる文章";
        assertTrue(nlpSimilarityService.calculateSimilarity(text1, text2) < 0.5, "Completely different texts should have low similarity");
    }

    @Test
    void testMinorVariationsParticlesAndEndings() throws IOException {
        String text1 = "私は昨日公園に行きました"; // I went to the park yesterday
        String text2 = "私、昨日公園へ行ったよ";   // I, yesterday, to the park, went (casual)
        // Expect high similarity due to tokenization and filtering of particles/auxiliary verbs
        assertTrue(nlpSimilarityService.calculateSimilarity(text1, text2) > 0.7, "Texts with minor variations (particles, endings) should have high similarity");
    }

    @Test
    void testWordOrderDifference() throws IOException {
        String text1 = "猫が魚を食べる"; // Cat eats fish
        String text2 = "魚を猫が食べる"; // Fish is eaten by cat (passive voice, different order)
        // Jaccard similarity is order-independent
        assertEquals(1.0, nlpSimilarityService.calculateSimilarity(text1, text2), "Texts with different word order but same tokens should have high similarity");
    }

    @Test
    void testSynonymLikeOrBaseFormNormalization() throws IOException {
        String text1 = "実行する"; // Execute (suru verb)
        String text2 = "実行しろ"; // Execute (imperative)
        // Kuromoji should normalize these to the same base form "実行"
        // "する" is an auxiliary verb and should be filtered. "しろ" is a conjugation of "する".
        // So we expect "実行" vs "実行"
        assertTrue(nlpSimilarityService.calculateSimilarity(text1, text2) >= 0.9, "Texts that normalize to similar base forms should have high similarity");
    }

    @Test
    void testOneTextEmpty() throws IOException {
        String text1 = "これはテストです";
        String text2 = "";
        assertEquals(0.0, nlpSimilarityService.calculateSimilarity(text1, text2), "Similarity with empty string should be 0.0");
    }

    @Test
    void testBothTextsEmpty() throws IOException {
        String text1 = "";
        String text2 = "";
        assertEquals(1.0, nlpSimilarityService.calculateSimilarity(text1, text2), "Similarity of two empty strings should be 1.0");
    }

    @Test
    void testNullInputs() throws IOException {
        assertEquals(0.0, nlpSimilarityService.calculateSimilarity(null, "text"), "Null first text should result in 0.0 similarity");
        assertEquals(0.0, nlpSimilarityService.calculateSimilarity("text", null), "Null second text should result in 0.0 similarity");
        assertEquals(0.0, nlpSimilarityService.calculateSimilarity(null, null), "Both null texts should result in 0.0 similarity");
    }

    @Test
    void testParticleAndAuxiliaryVerbFiltering() throws IOException {
        // "食べる" (eat) vs "食べます" (eat - polite) -> base form "食べる"
        // "の" particle, "です" auxiliary verb
        String text1 = "猫が魚を食べるのです"; // The cat eats fish (explanatory nuance)
        String text2 = "猫は魚を食べます";   // The cat eats fish (polite)
        // After tokenization and filtering: {猫, 魚, 食べる} vs {猫, 魚, 食べる}
        assertTrue(nlpSimilarityService.calculateSimilarity(text1, text2) > 0.9, "Texts should be similar after filtering particles and auxiliary verbs");
    }

    @Test
    void testContentWordsMixedWithStopWords() throws IOException {
        String text1 = "重要な情報を含みます"; // Contains important information
        String text2 = "重要な情報のみ";     // Only important information
        // Should focus on "重要" and "情報"
        assertTrue(nlpSimilarityService.calculateSimilarity(text1, text2) > 0.4, "Similarity should be based on content words");
    }
}
