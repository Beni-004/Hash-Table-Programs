import java.util.*;
import java.util.concurrent.*;
public class Problem3_DNSCache {
    static class DNSEntry {
        String domain;
        String ipAddress;
        long createdAt;
        long expiryTime; 
        DNSEntry(String domain, String ipAddress, long ttlSeconds) {
            this.domain = domain;
            this.ipAddress = ipAddress;
            this.createdAt = System.currentTimeMillis();
            this.expiryTime = createdAt + (ttlSeconds * 1000);
        }
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
        long remainingTTL() {
            return Math.max(0, (expiryTime - System.currentTimeMillis()) / 1000);
        }
    }
    private static final int MAX_CACHE_SIZE = 1000;
    private final LinkedHashMap<String, DNSEntry> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, DNSEntry> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    private long hits = 0;
    private long misses = 0;
    private long totalLookupTimeNs = 0;
    private long lookupCount = 0;
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
    public Problem3_DNSCache() {
        cleaner.scheduleAtFixedRate(this::removeExpiredEntries, 60, 60, TimeUnit.SECONDS);
    }
    private String queryUpstreamDNS(String domain) {
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        int hash = Math.abs(domain.hashCode());
        return (hash % 256) + "." + ((hash >> 8) % 256) + "." + ((hash >> 16) % 256) + ".1";
    }
    public String resolve(String domain) {
        long start = System.nanoTime();
        String result;
        synchronized (cache) {
            DNSEntry entry = cache.get(domain);
            if (entry != null && !entry.isExpired()) {
                hits++;
                result = entry.ipAddress;
                long elapsed = System.nanoTime() - start;
                totalLookupTimeNs += elapsed;
                lookupCount++;
                System.out.printf("resolve(\"%s\") -> Cache HIT -> %s (retrieved in %.2fms)%n",
                        domain, result, elapsed / 1_000_000.0);
                return result;
            }
            if (entry != null) {
                cache.remove(domain);
                System.out.printf("resolve(\"%s\") -> Cache EXPIRED -> Querying upstream...%n", domain);
            } else {
                System.out.printf("resolve(\"%s\") -> Cache MISS -> Querying upstream...%n", domain);
            }
            misses++;
        }
        result = queryUpstreamDNS(domain);
        long ttl = 300L; 
        synchronized (cache) {
            cache.put(domain, new DNSEntry(domain, result, ttl));
        }
        long elapsed = System.nanoTime() - start;
        totalLookupTimeNs += elapsed;
        lookupCount++;
        System.out.printf("  -> Got %s (TTL: %ds, total time: %.1fms)%n", result, ttl, elapsed / 1_000_000.0);
        return result;
    }
    public void addEntry(String domain, String ip, long ttlSeconds) {
        synchronized (cache) {
            cache.put(domain, new DNSEntry(domain, ip, ttlSeconds));
        }
    }
    public void invalidate(String domain) {
        synchronized (cache) {
            cache.remove(domain);
        }
        System.out.println("Invalidated cache for: " + domain);
    }
    private void removeExpiredEntries() {
        synchronized (cache) {
            cache.entrySet().removeIf(e -> e.getValue().isExpired());
        }
    }
    public void getCacheStats() {
        long total = hits + misses;
        double hitRate = total == 0 ? 0 : (hits * 100.0 / total);
        double avgTime = lookupCount == 0 ? 0 : (totalLookupTimeNs / 1_000_000.0 / lookupCount);
        System.out.println("\n=== Cache Statistics ===");
        System.out.printf("Hit Rate: %.1f%% (%d hits / %d total)%n", hitRate, hits, total);
        System.out.printf("Avg Lookup Time: %.2fms%n", avgTime);
        System.out.printf("Cache Size: %d entries%n", cache.size());
    }
    public void shutdown() {
        cleaner.shutdown();
    }
    public static void main(String[] args) throws InterruptedException {
        Problem3_DNSCache dns = new Problem3_DNSCache();
        System.out.println("=== Problem 3: DNS Cache with TTL ===\n");
        dns.resolve("google.com");
        System.out.println();
        dns.resolve("google.com");
        System.out.println();
        dns.resolve("github.com");
        dns.resolve("github.com");
        dns.resolve("stackoverflow.com");
        System.out.println();
        dns.addEntry("shortlived.com", "1.2.3.4", 1);
        dns.resolve("shortlived.com"); 
        System.out.println("Waiting 2 seconds for TTL to expire...");
        Thread.sleep(2000);
        dns.resolve("shortlived.com"); 
        dns.getCacheStats();
        dns.shutdown();
    }
}