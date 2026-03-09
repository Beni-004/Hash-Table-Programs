import java.util.*;
public class Problem7_AutocompleteSystem {
    static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord = false;
        String fullQuery = null; 
    }
    private final Map<String, Integer> frequencies = new HashMap<>();
    private final Map<String, List<String>> prefixCache = new HashMap<>();
    private final TrieNode root = new TrieNode();
    private static final int TOP_K = 10;
    private static final int CACHE_SIZE = 500; 
    public void insertQuery(String query, int frequency) {
        query = query.toLowerCase().trim();
        frequencies.put(query, frequency);
        insertIntoTrie(query);
    }
    private void insertIntoTrie(String query) {
        TrieNode node = root;
        for (char c : query.toCharArray()) {
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);
        }
        node.isEndOfWord = true;
        node.fullQuery = query;
    }
    public void updateFrequency(String query) {
        query = query.toLowerCase().trim();
        int newFreq = frequencies.merge(query, 1, Integer::sum);
        if (newFreq == 1) insertIntoTrie(query);
        for (int i = 1; i <= query.length(); i++) {
            prefixCache.remove(query.substring(0, i));
        }
        System.out.printf("updateFrequency(\"%s\") -> Frequency: %d (trending: %s)%n",
                query, newFreq, newFreq <= 5 ? "yes" : "no");
    }
    public List<String> search(String prefix) {
        prefix = prefix.toLowerCase().trim();
        if (prefixCache.containsKey(prefix)) {
            List<String> cached = prefixCache.get(prefix);
            System.out.printf("search(\"%s\") -> [CACHED] %d suggestions%n", prefix, cached.size());
            return cached;
        }
        TrieNode node = root;
        for (char c : prefix.toCharArray()) {
            if (!node.children.containsKey(c)) {
                System.out.printf("search(\"%s\") -> No suggestions found%n", prefix);
                return Collections.emptyList();
            }
            node = node.children.get(c);
        }
        List<String> allMatches = new ArrayList<>();
        collectAllQueries(node, allMatches);
        allMatches.sort((a, b) ->
                frequencies.getOrDefault(b, 0) - frequencies.getOrDefault(a, 0));
        List<String> topK = allMatches.subList(0, Math.min(TOP_K, allMatches.size()));
        if (prefixCache.size() >= CACHE_SIZE) {
            prefixCache.remove(prefixCache.keySet().iterator().next());
        }
        prefixCache.put(prefix, topK);
        System.out.printf("search(\"%s\") ->%n", prefix);
        for (int i = 0; i < topK.size(); i++) {
            String q = topK.get(i);
            System.out.printf("  %d. \"%s\" (%,d searches)%n",
                    i + 1, q, frequencies.getOrDefault(q, 0));
        }
        return topK;
    }
    private void collectAllQueries(TrieNode node, List<String> results) {
        if (node.isEndOfWord) results.add(node.fullQuery);
        for (TrieNode child : node.children.values()) {
            collectAllQueries(child, results);
        }
    }
    public List<String> suggestCorrections(String typo) {
        typo = typo.toLowerCase();
        List<Map.Entry<String, Integer>> candidates = new ArrayList<>();
        for (String query : frequencies.keySet()) {
            if (!query.isEmpty() && !typo.isEmpty() &&
                    query.charAt(0) == typo.charAt(0)) { 
                int dist = editDistance(typo, query);
                if (dist <= 2) { 
                    candidates.add(Map.entry(query, frequencies.getOrDefault(query, 0)));
                }
            }
        }
        candidates.sort((a, b) -> b.getValue() - a.getValue());
        List<String> corrections = new ArrayList<>();
        for (int i = 0; i < Math.min(3, candidates.size()); i++) {
            corrections.add(candidates.get(i).getKey());
        }
        System.out.printf("suggestCorrections(\"%s\") -> %s%n", typo, corrections);
        return corrections;
    }
    private int editDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1]
                        : 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
        return dp[a.length()][b.length()];
    }
    public static void main(String[] args) {
        Problem7_AutocompleteSystem ac = new Problem7_AutocompleteSystem();
        System.out.println("=== Problem 7: Autocomplete System ===\n");
        ac.insertQuery("java tutorial", 1_234_567);
        ac.insertQuery("javascript", 987_654);
        ac.insertQuery("java download", 456_789);
        ac.insertQuery("java 21 features", 1);
        ac.insertQuery("java stream api", 345_678);
        ac.insertQuery("java interview questions", 289_000);
        ac.insertQuery("javascript tutorial", 876_543);
        ac.insertQuery("javascript vs typescript", 234_567);
        ac.insertQuery("python tutorial", 1_100_000);
        ac.insertQuery("python download", 890_000);
        ac.insertQuery("python vs java", 450_000);
        ac.insertQuery("react tutorial", 780_000);
        System.out.println("--- Searching for prefix 'jav' ---");
        ac.search("jav");
        System.out.println("\n--- Searching for prefix 'java ' ---");
        ac.search("java ");
        System.out.println("\n--- Trending query update ---");
        for (int i = 0; i < 3; i++) ac.updateFrequency("java 21 features");
        System.out.println("\n--- Search 'java ' again (cache invalidated) ---");
        ac.search("java ");
        System.out.println("\n--- Typo correction ---");
        ac.suggestCorrections("pythn");
        ac.suggestCorrections("javasript");
    }
}