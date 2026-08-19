package com.example.telegramvideo.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RateLimitServiceTest {

    private static final Long CHAT_ID = 42L;
    private static final Long OTHER_CHAT_ID = 43L;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-08-20T10:00:00Z"));
    private final RateLimitService service = new RateLimitService(new RateLimitProperties(3), clock);

    @Test
    void allowsRequestsUpToTheLimit() {
        assertThat(service.tryAcquire(CHAT_ID)).isTrue();
        assertThat(service.tryAcquire(CHAT_ID)).isTrue();
        assertThat(service.tryAcquire(CHAT_ID)).isTrue();
    }

    @Test
    void blocksRequestsOverTheLimit() {
        for (int i = 0; i < 3; i++) {
            service.tryAcquire(CHAT_ID);
        }

        assertThat(service.tryAcquire(CHAT_ID)).isFalse();
        assertThat(service.tryAcquire(CHAT_ID)).isFalse();
    }

    @Test
    void countsEachChatSeparately() {
        for (int i = 0; i < 3; i++) {
            service.tryAcquire(CHAT_ID);
        }

        assertThat(service.tryAcquire(CHAT_ID)).isFalse();
        assertThat(service.tryAcquire(OTHER_CHAT_ID)).isTrue();
    }

    @Test
    void allowsRequestsAgainAfterTheWindowPasses() {
        for (int i = 0; i < 4; i++) {
            service.tryAcquire(CHAT_ID);
        }
        assertThat(service.tryAcquire(CHAT_ID)).isFalse();

        clock.advance(Duration.ofSeconds(61));

        assertThat(service.tryAcquire(CHAT_ID)).isTrue();
    }

    @Test
    void keepsBlockingInsideTheSameWindow() {
        for (int i = 0; i < 4; i++) {
            service.tryAcquire(CHAT_ID);
        }

        clock.advance(Duration.ofSeconds(59));

        assertThat(service.tryAcquire(CHAT_ID)).isFalse();
    }

    private static class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
