package com.example.telegramvideo.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param maxRequestsPerMinute how many requests one chat may send per minute
 */
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(int maxRequestsPerMinute) {
}
