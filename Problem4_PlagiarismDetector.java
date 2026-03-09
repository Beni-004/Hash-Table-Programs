import java.util.*;
public class Problem4_PlagiarismDetector {
    private static final int N_GRAM_SIZE = 5;
    private static final double PLAGIARISM_THRESHOLD = 0.5;
    private static final double SUSPICIOUS_THRESHOLD = 0.1;
    private final Map<String, Set<String>> ngramIndex = new HashMap<>();
    private final Map<String, List<String>> documentNgrams = new HashMap<>();
    public void indexDocument(String docId, String content) {
        List<String> ngrams = extractNgrams(content);
        documentNgrams.put(docId, ngrams);
        for (String ngram : ngrams) {
            ngramIndex.computeIfAbsent(ngram, k -> new HashSet<>()).add(docId);
        }
        System.out.printf("Indexed \"%s\" -> %d n-grams extracted%n", docId, ngrams.size());
    }
    private List<String> extractNgrams(String content) {
        String[] words = content.toLowerCase().replaceAll("[^a-z0-9 ]", "").split("\\s+");
        List<String> ngrams = new ArrayList<>();
        for (int i = 0; i <= words.length - N_GRAM_SIZE; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = i; j < i + N_GRAM_SIZE; j++) {
                if (j > i) sb.append(" ");
                sb.append(words[j]);
            }
            ngrams.add(sb.toString());
        }
        return ngrams;
    }
    public Map<String, Double> analyzeDocument(String docId) {
        List<String> targetNgrams = documentNgrams.get(docId);
        if (targetNgrams == null) {
            System.out.println("Document not indexed: " + docId);
            return Collections.emptyMap();
        }
        Map<String, Integer> matchCounts = new HashMap<>();
        for (String ngram : targetNgrams) {
            Set<String> docs = ngramIndex.getOrDefault(ngram, Collections.emptySet());
            for (String otherDoc : docs) {
                if (!otherDoc.equals(docId)) {
                    matchCounts.merge(otherDoc, 1, Integer::sum);
                }
            }
        }
        Map<String, Double> similarities = new LinkedHashMap<>();
        int totalNgrams = targetNgrams.size();
        System.out.printf("%nanalyzeDocument(\"%s\")%n", docId);
        System.out.printf("  -> Extracted %d n-grams%n", totalNgrams);
        matchCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    double similarity = (entry.getValue() * 100.0) / totalNgrams;
                    similarities.put(entry.getKey(), similarity);
                    String verdict;
                    if (similarity >= PLAGIARISM_THRESHOLD * 100) verdict = "PLAGIARISM DETECTED";
                    else if (similarity >= SUSPICIOUS_THRESHOLD * 100) verdict = "suspicious";
                    else verdict = "likely original";
                    System.out.printf("  -> Found %d matching n-grams with \"%s\"%n",
                            entry.getValue(), entry.getKey());
                    System.out.printf("  -> Similarity: %.1f%% (%s)%n", similarity, verdict);
                });
        if (similarities.isEmpty()) {
            System.out.println("  -> No matching documents found. Original content.");
        }
        return similarities;
    }
    public void findMostSimilarPair() {
        List<String> docs = new ArrayList<>(documentNgrams.keySet());
        String bestA = null, bestB = null;
        double bestSim = 0;
        for (int i = 0; i < docs.size(); i++) {
            for (int j = i + 1; j < docs.size(); j++) {
                String a = docs.get(i), b = docs.get(j);
                List<String> aNgrams = documentNgrams.get(a);
                Set<String> bNgramSet = new HashSet<>(documentNgrams.get(b));
                long matches = aNgrams.stream().filter(bNgramSet::contains).count();
                double sim = (matches * 100.0) / aNgrams.size();
                if (sim > bestSim) { bestSim = sim; bestA = a; bestB = b; }
            }
        }
        if (bestA != null) {
            System.out.printf("%nMost similar pair: \"%s\" & \"%s\" (%.1f%% similarity)%n",
                    bestA, bestB, bestSim);
        }
    }
    public static void main(String[] args) {
        Problem4_PlagiarismDetector detector = new Problem4_PlagiarismDetector();
        System.out.println("=== Problem 4: Plagiarism Detection System ===\n");
        String essay1 = "The quick brown fox jumps over the lazy dog near the river bank. " +
                "Science has shown that animals in the wild often demonstrate remarkable intelligence. " +
                "Studies conducted over many decades reveal fascinating behavioral patterns in nature.";
        String essay2 = "The quick brown fox jumps over the lazy dog near the river bank. " +
                "Science has shown that animals in the wild often demonstrate remarkable intelligence. " +
                "Modern research confirms significant adaptive strategies in wild populations.";
        String essay3 = "Studies conducted over many decades reveal fascinating behavioral patterns. " +
                "Many researchers have explored the nature of animal cognition and learning ability. " +
                "Environmental factors play a crucial role in the development of animal behavior.";
        String essay4 = "The history of computing dates back to ancient times when humans used abacus tools. " +
                "Modern digital computers were invented in the twentieth century by pioneering engineers. " +
                "Today artificial intelligence transforms industries around the world with new innovations.";
        detector.indexDocument("essay_001.txt", essay1);
        detector.indexDocument("essay_089.txt", essay3);
        detector.indexDocument("essay_092.txt", essay2);
        detector.indexDocument("essay_new.txt", essay4);
        detector.analyzeDocument("essay_001.txt");
        detector.analyzeDocument("essay_new.txt");
        detector.findMostSimilarPair();
    }
}