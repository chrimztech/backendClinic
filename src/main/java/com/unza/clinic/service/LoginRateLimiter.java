package com.unza.clinic.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple sliding-window rate limiter for login attempts.
 * Tracks failed attempts per IP in memory; no external dependency needed.
 */
@Service
public class LoginRateLimiter {

    @Value("${app.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.login.window-seconds:60}")
    private int windowSeconds;

    // IP → timestamps (millis) of recent FAILED attempts
    private final Map<String, Deque<Long>> failureWindow = new ConcurrentHashMap<>();

    /** Returns true if the IP is currently rate-limited (too many recent failures). */
    public boolean isBlocked(String ip) {
        Deque<Long> attempts = failureWindow.get(ip);
        if (attempts == null) return false;
        long cutoff = System.currentTimeMillis() - (windowSeconds * 1000L);
        synchronized (attempts) {
            attempts.removeIf(t -> t < cutoff);
            return attempts.size() >= maxAttempts;
        }
    }

    /** Record a failed login attempt for this IP. */
    public void recordFailure(String ip) {
        failureWindow.computeIfAbsent(ip, k -> new ArrayDeque<>());
        Deque<Long> attempts = failureWindow.get(ip);
        synchronized (attempts) {
            attempts.addLast(System.currentTimeMillis());
            // Cap stored timestamps to avoid unbounded memory growth
            while (attempts.size() > maxAttempts * 2) attempts.pollFirst();
        }
    }

    /** Clear the failure record for this IP (call on successful login). */
    public void clearFailures(String ip) {
        failureWindow.remove(ip);
    }

    /** Returns how many seconds remain until this IP is unblocked. */
    public long retryAfterSeconds(String ip) {
        Deque<Long> attempts = failureWindow.get(ip);
        if (attempts == null || attempts.isEmpty()) return 0;
        Long oldestInWindow;
        synchronized (attempts) {
            oldestInWindow = attempts.peekFirst();
        }
        if (oldestInWindow == null) return 0;
        long unblockAt = oldestInWindow + (windowSeconds * 1000L);
        long remaining = (unblockAt - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    /** Purge stale entries every 5 minutes to prevent memory leak. */
    @Scheduled(fixedDelay = 300_000)
    public void purgeStale() {
        long cutoff = System.currentTimeMillis() - (windowSeconds * 1000L);
        failureWindow.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                entry.getValue().removeIf(t -> t < cutoff);
                return entry.getValue().isEmpty();
            }
        });
    }
}
