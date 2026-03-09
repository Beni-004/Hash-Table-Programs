import java.util.*;
import java.util.stream.Collectors;
public class Problem5_AnalyticsDashboard {
    static class PageViewEvent {
        String url;
        String userId;
        String source;
        long timestamp;
        PageViewEvent(String url, String userId, String source) {
            this.url = url;
            this.userId = userId;
            this.source = source;
            this.timestamp = System.currentTimeMillis();
        }
    }
    private final Map<String, Long> pageViews = new HashMap<>();
    private final Map<String, Set<String>> uniqueVisitors = new HashMap<>();
    private final Map<String, Long> trafficSources = new HashMap<>();
    private final Map<String, List<String>> userSessions = new HashMap<>();
    private long totalEvents = 0;
    private long batchStartTime = System.currentTimeMillis();
    private final List<PageViewEvent> pendingBatch = new ArrayList<>();
    private static final long BATCH_INTERVAL_MS = 5000; 
    public void processEvent(PageViewEvent event) {
        pendingBatch.add(event);
        if (System.currentTimeMillis() - batchStartTime >= BATCH_INTERVAL_MS) {
            flushBatch();
        }
    }
    public void flushBatch() {
        for (PageViewEvent event : pendingBatch) {
            pageViews.merge(event.url, 1L, Long::sum);
            uniqueVisitors.computeIfAbsent(event.url, k -> new HashSet<>()).add(event.userId);
            trafficSources.merge(event.source, 1L, Long::sum);
            userSessions.computeIfAbsent(event.userId, k -> new ArrayList<>()).add(event.url);
            totalEvents++;
        }
        pendingBatch.clear();
        batchStartTime = System.currentTimeMillis();
    }
    public List<Map.Entry<String, Long>> getTopPages(int n) {
        return pageViews.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n)
                .collect(Collectors.toList());
    }
    public int getUniqueVisitors(String url) {
        return uniqueVisitors.getOrDefault(url, Collections.emptySet()).size();
    }
    public Map<String, String> getTrafficSourceBreakdown() {
        long total = trafficSources.values().stream().mapToLong(Long::longValue).sum();
        Map<String, String> breakdown = new LinkedHashMap<>();
        trafficSources.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> breakdown.put(e.getKey(),
                        String.format("%.0f%%", e.getValue() * 100.0 / total)));
        return breakdown;
    }
    public double getBounceRate() {
        long bounced = userSessions.values().stream().filter(s -> s.size() == 1).count();
        return userSessions.isEmpty() ? 0 : (bounced * 100.0 / userSessions.size());
    }
    public void getDashboard() {
        flushBatch(); 
        System.out.println("\n=== Real-Time Analytics Dashboard ===");
        System.out.println("Total Events Processed: " + totalEvents);
        System.out.println("\nTop Pages:");
        List<Map.Entry<String, Long>> top = getTopPages(5);
        for (int i = 0; i < top.size(); i++) {
            Map.Entry<String, Long> entry = top.get(i);
            int unique = getUniqueVisitors(entry.getKey());
            System.out.printf("  %d. %s - %,d views (%,d unique)%n",
                    i + 1, entry.getKey(), entry.getValue(), unique);
        }
        System.out.println("\nTraffic Sources:");
        getTrafficSourceBreakdown().forEach((source, pct) ->
                System.out.printf("  %s: %s%n", source, pct));
        System.out.printf("%nBounce Rate: %.1f%%%n", getBounceRate());
    }
    public static void main(String[] args) {
        Problem5_AnalyticsDashboard dashboard = new Problem5_AnalyticsDashboard();
        System.out.println("=== Problem 5: Real-Time Analytics Dashboard ===\n");
        String[] pages = {"/article/breaking-news", "/sports/championship", "/tech/ai-update",
                "/politics/election", "/entertainment/movies"};
        String[] sources = {"google", "facebook", "direct", "twitter", "other"};
        Random random = new Random(42);
        int[] weights = {5, 4, 3, 2, 1}; 
        for (int i = 0; i < 500; i++) {
            int rand = random.nextInt(15);
            int pageIdx = rand < 5 ? 0 : rand < 9 ? 1 : rand < 12 ? 2 : rand < 14 ? 3 : 4;
            int srcRand = random.nextInt(10);
            int srcIdx = srcRand < 4 ? 0 : srcRand < 7 ? 2 : srcRand < 9 ? 1 : srcRand < 9 ? 3 : 4;
            String userId = "user_" + (random.nextInt(200) + 1);
            dashboard.processEvent(new PageViewEvent(pages[pageIdx], userId, sources[srcIdx]));
        }
        dashboard.getDashboard();
    }
}