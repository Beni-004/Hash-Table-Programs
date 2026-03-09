import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
public class Problem10_MultiLevelCache {
    static class VideoData {
        String videoId;
        String title;
        byte[] thumbnailData; 
        long sizeBytes;
        VideoData(String videoId) {
            this.videoId = videoId;
            this.title = "Video: " + videoId;
            this.sizeBytes = 1024 * (50 + new Random(videoId.hashCode()).nextInt(200)); 
        }
        @Override
        public String toString() {
            return String.format("VideoData{id='%s', size=%dKB}", videoId, sizeBytes / 1024);
        }
    }
    private final int L1_CAPACITY = 10;
    private final LinkedHashMap<String, VideoData> l1Cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, VideoData> eldest) {
            if (size() > L1_CAPACITY) {
                l2Cache.put(eldest.getKey(), eldest.getValue());
                return true;
            }
            return false;
        }
    };
    private final int L2_CAPACITY = 30;
    private final LinkedHashMap<String, VideoData> l2Cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, VideoData> eldest) {
            return size() > L2_CAPACITY; 
        }
    };
    private final Map<String, VideoData> l3Database = new HashMap<>();
    private final Map<String, Integer> accessCount = new HashMap<>();
    private static final int PROMOTION_THRESHOLD = 3; 
    private final long[] hits = new long[3];
    private final long[] misses = new long[3];
    private final long[] totalTimeNs = new long[3];
    private final long[] requests = new long[3];
    public Problem10_MultiLevelCache() {
        for (int i = 1; i <= 200; i++) {
            String id = "video_" + i;
            l3Database.put(id, new VideoData(id));
        }
        for (int i = 1; i <= 10; i++) l1Cache.put("video_" + i, l3Database.get("video_" + i));
        for (int i = 11; i <= 30; i++) l2Cache.put("video_" + i, l3Database.get("video_" + i));
    }
    public VideoData getVideo(String videoId) {
        System.out.printf("getVideo(\"%s\")%n", videoId);
        long startTotal = System.nanoTime();
        long t1 = System.nanoTime();
        VideoData data = l1Cache.get(videoId);
        long l1Time = System.nanoTime() - t1;
        if (data != null) {
            hits[0]++;
            totalTimeNs[0] += l1Time;
            requests[0]++;
            System.out.printf("  -> L1 Cache HIT (%.2fms)%n", l1Time / 1_000_000.0);
            accessCount.merge(videoId, 1, Integer::sum);
            return data;
        }
        misses[0]++;
        requests[0]++;
        System.out.printf("  -> L1 Cache MISS (%.2fms)%n", l1Time / 1_000_000.0);
        long t2 = System.nanoTime();
        simulateLatency(2); 
        data = l2Cache.get(videoId);
        long l2Time = System.nanoTime() - t2;
        if (data != null) {
            hits[1]++;
            totalTimeNs[1] += l2Time;
            requests[1]++;
            System.out.printf("  -> L2 Cache HIT (%.1fms)%n", l2Time / 1_000_000.0);
            int count = accessCount.merge(videoId, 1, Integer::sum);
            if (count >= PROMOTION_THRESHOLD) {
                l1Cache.put(videoId, data);
                System.out.printf("  -> Promoted to L1 (access count: %d >= threshold %d)%n",
                        count, PROMOTION_THRESHOLD);
            }
            long total = System.nanoTime() - startTotal;
            System.out.printf("  -> Total: %.1fms%n", total / 1_000_000.0);
            return data;
        }
        misses[1]++;
        requests[1]++;
        System.out.printf("  -> L2 Cache MISS (%.1fms)%n", l2Time / 1_000_000.0);
        long t3 = System.nanoTime();
        simulateLatency(50); 
        data = l3Database.get(videoId);
        long l3Time = System.nanoTime() - t3;
        if (data != null) {
            hits[2]++;
            totalTimeNs[2] += l3Time;
            requests[2]++;
            System.out.printf("  -> L3 Database HIT (%.0fms)%n", l3Time / 1_000_000.0);
            l2Cache.put(videoId, data);
            accessCount.put(videoId, 1);
            System.out.printf("  -> Added to L2 (access count: 1)%n");
        } else {
            misses[2]++;
            requests[2]++;
            System.out.printf("  -> L3 NOT FOUND%n");
        }
        long total = System.nanoTime() - startTotal;
        System.out.printf("  -> Total: %.0fms%n", total / 1_000_000.0);
        return data;
    }
    public void invalidate(String videoId) {
        l1Cache.remove(videoId);
        l2Cache.remove(videoId);
        accessCount.remove(videoId);
        System.out.printf("Invalidated \"%s\" from all cache levels%n", videoId);
    }
    private void simulateLatency(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
    public void getStatistics() {
        System.out.println("\n=== Cache Statistics ===");
        String[] levels = {"L1 (Memory)", "L2 (SSD)", "L3 (Database)"};
        long overallHits = 0, overallRequests = 0;
        double weightedTime = 0;
        for (int i = 0; i < 3; i++) {
            long totalReq = hits[i] + misses[i];
            double hitRate = totalReq == 0 ? 0 : (hits[i] * 100.0 / totalReq);
            double avgTime = requests[i] == 0 ? 0 : (totalTimeNs[i] / 1_000_000.0 / requests[i]);
            overallHits += hits[i];
            overallRequests += totalReq;
            weightedTime += totalTimeNs[i] / 1_000_000.0;
            System.out.printf("%s: Hit Rate %.0f%%, Avg Time: %.1fms (%d hits / %d reqs)%n",
                    levels[i], hitRate, avgTime, hits[i], totalReq);
        }
        double overallHitRate = overallRequests == 0 ? 0 : (overallHits * 100.0 / overallRequests);
        System.out.printf("Overall: Hit Rate %.0f%%, L1 size: %d, L2 size: %d%n",
                overallHitRate, l1Cache.size(), l2Cache.size());
    }
    public static void main(String[] args) throws InterruptedException {
        Problem10_MultiLevelCache cache = new Problem10_MultiLevelCache();
        System.out.println("=== Problem 10: Multi-Level Cache System ===\n");
        System.out.println("--- Scenario: L2 hit ---");
        cache.getVideo("video_20"); 
        System.out.println();
        System.out.println("--- Scenario: L1 hit (cached from previous) ---");
        cache.getVideo("video_5"); 
        System.out.println();
        System.out.println("--- Scenario: Full miss (L1 MISS -> L2 MISS -> L3 HIT) ---");
        cache.getVideo("video_100");
        System.out.println();
        System.out.println("--- Scenario: Promoting video_50 from L2 to L1 ---");
        cache.getVideo("video_50");
        cache.getVideo("video_50");
        cache.getVideo("video_50"); 
        System.out.println();
        cache.getVideo("video_50"); 
        System.out.println();
        System.out.println("--- Scenario: Content update -> invalidate ---");
        cache.invalidate("video_5");
        cache.getVideo("video_5"); 
        System.out.println();
        cache.getStatistics();
    }
}