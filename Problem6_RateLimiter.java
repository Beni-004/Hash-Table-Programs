import java.util.*;
import java.util.concurrent.*;
public class Problem6_RateLimiter {
    static class TokenBucket {
        final int maxTokens;
        final double refillRatePerMs; 
        double tokens;
        long lastRefillTime;
        TokenBucket(int maxTokens, long windowMs) {
            this.maxTokens = maxTokens;
            this.refillRatePerMs = (double) maxTokens / windowMs;
            this.tokens = maxTokens; 
            this.lastRefillTime = System.currentTimeMillis();
        }
        synchronized void refill() {
            long now = System.currentTimeMillis();
            double elapsed = now - lastRefillTime;
            tokens = Math.min(maxTokens, tokens + elapsed * refillRatePerMs);
            lastRefillTime = now;
        }
        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
        synchronized int remainingTokens() {
            refill();
            return (int) tokens;
        }
        synchronized long retryAfterSeconds() {
            refill();
            if (tokens >= 1.0) return 0;
            double tokensNeeded = 1.0 - tokens;
            return (long) Math.ceil(tokensNeeded / refillRatePerMs / 1000);
        }
        synchronized long resetEpochSecond() {
            return (lastRefillTime + (long)((maxTokens - tokens) / refillRatePerMs)) / 1000;
        }
    }
    private static final int DEFAULT_LIMIT = 1000;          
    private static final long WINDOW_MS = 60 * 60 * 1000L; 
    private final ConcurrentHashMap<String, TokenBucket> clients = new ConcurrentHashMap<>();
    public RateLimitResult checkRateLimit(String clientId) {
        TokenBucket bucket = clients.computeIfAbsent(clientId,
                k -> new TokenBucket(DEFAULT_LIMIT, WINDOW_MS));
        boolean allowed = bucket.tryConsume();
        int remaining = bucket.remainingTokens();
        if (allowed) {
            System.out.printf("checkRateLimit(\"%s\") -> Allowed (%d requests remaining)%n",
                    clientId, remaining);
        } else {
            long retryAfter = bucket.retryAfterSeconds();
            System.out.printf("checkRateLimit(\"%s\") -> Denied (0 requests remaining, retry after %ds)%n",
                    clientId, retryAfter);
        }
        return new RateLimitResult(allowed, remaining, bucket.resetEpochSecond());
    }
    public void getRateLimitStatus(String clientId) {
        TokenBucket bucket = clients.get(clientId);
        if (bucket == null) {
            System.out.printf("getRateLimitStatus(\"%s\") -> No data (never used)%n", clientId);
            return;
        }
        int used = DEFAULT_LIMIT - bucket.remainingTokens();
        System.out.printf("getRateLimitStatus(\"%s\") -> {used: %d, limit: %d, reset: %d}%n",
                clientId, used, DEFAULT_LIMIT, bucket.resetEpochSecond());
    }
    public void registerClient(String clientId, int customLimit) {
        clients.put(clientId, new TokenBucket(customLimit, WINDOW_MS));
        System.out.printf("Registered client \"%s\" with limit %d req/hour%n", clientId, customLimit);
    }
    static class RateLimitResult {
        boolean allowed;
        int remaining;
        long resetEpochSecond;
        RateLimitResult(boolean allowed, int remaining, long resetEpochSecond) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.resetEpochSecond = resetEpochSecond;
        }
    }
    public void benchmark(String clientId, int requests) {
        long start = System.nanoTime();
        for (int i = 0; i < requests; i++) {
            clients.computeIfAbsent(clientId + i, k -> new TokenBucket(DEFAULT_LIMIT, WINDOW_MS))
                    .tryConsume();
        }
        long elapsed = System.nanoTime() - start;
        System.out.printf("Benchmark: %d checks in %.2fms (avg %.3fms each)%n",
                requests, elapsed / 1_000_000.0, elapsed / 1_000_000.0 / requests);
    }
    public static void main(String[] args) throws InterruptedException {
        Problem6_RateLimiter limiter = new Problem6_RateLimiter();
        System.out.println("=== Problem 6: Distributed Rate Limiter ===\n");
        limiter.registerClient("premium_client", 5000);
        System.out.println();
        limiter.registerClient("abc123", 5);
        limiter.checkRateLimit("abc123");
        limiter.checkRateLimit("abc123");
        limiter.checkRateLimit("abc123");
        limiter.checkRateLimit("abc123");
        limiter.checkRateLimit("abc123");
        limiter.checkRateLimit("abc123");
        System.out.println();
        limiter.getRateLimitStatus("abc123");
        System.out.println();
        limiter.checkRateLimit("different_client"); 
        System.out.println();
        limiter.benchmark("bench_client", 10000);
    }
}