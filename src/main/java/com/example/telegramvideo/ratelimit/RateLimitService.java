package com.example.telegramvideo.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * In-memory per-chat rate limiting with a fixed one minute window.
 */
@Service
public class RateLimitService {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RateLimitProperties properties;
    private final Clock clock;
    private final Map<Long, Window> windows = new ConcurrentHashMap<>();

    public RateLimitService(RateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * @return true if the request is allowed, false if the chat is over its limit
     */
    public boolean tryAcquire(Long chatId) {
        Instant now = clock.instant();

        Window window = windows.compute(chatId, (id, current) -> {
            if (current == null || current.isExpired(now)) {
                return new Window(now, 1);
            }
            return new Window(current.startedAt(), current.count() + 1);
        });

        return window.count() <= properties.maxRequestsPerMinute();
    }

    private record Window(Instant startedAt, int count) {

        boolean isExpired(Instant now) {
            return !now.isBefore(startedAt.plus(WINDOW));
        }
    }
}
