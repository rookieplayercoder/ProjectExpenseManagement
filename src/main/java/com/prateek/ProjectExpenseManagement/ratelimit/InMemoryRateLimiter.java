package com.prateek.ProjectExpenseManagement.ratelimit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A minimal fixed-window rate limiter, keyed by an arbitrary string (e.g.
 * "login:203.0.113.5"). Intentionally has no external dependency - this is
 * in-memory only, so it resets on restart and does NOT coordinate across
 * multiple app instances. For a multi-instance deployment behind a load
 * balancer, replace this with a shared store (Redis, etc) instead.
 */
@Component
public class InMemoryRateLimiter {

    private static final long STALE_ENTRY_CLEANUP_MILLIS = 10 * 60 * 1000; // 10 min

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * @return true if the request is allowed, false if the caller has
     *         exceeded maxRequests within windowMillis and should be rejected.
     */
    public boolean tryConsume(String key, int maxRequests, long windowMillis) {
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(key, k -> new Window(now));

        synchronized (window) {
            if (now - window.startMillis.get() >= windowMillis) {
                // Window expired - start a fresh one.
                window.startMillis.set(now);
                window.count.set(0);
            }
            return window.count.incrementAndGet() <= maxRequests;
        }
    }

    // Prevents unbounded growth of the map from one-off/attacker IPs that
    // never come back - runs independently of any single request.
    @Scheduled(fixedDelay = STALE_ENTRY_CLEANUP_MILLIS)
    void evictStaleEntries() {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(entry -> now - entry.getValue().startMillis.get() > STALE_ENTRY_CLEANUP_MILLIS);
    }

    private static final class Window {
        final AtomicLong startMillis;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long startMillis) {
            this.startMillis = new AtomicLong(startMillis);
        }
    }
}
