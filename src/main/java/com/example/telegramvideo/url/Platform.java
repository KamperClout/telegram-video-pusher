package com.example.telegramvideo.url;

import java.util.List;
import java.util.Optional;

/**
 * Supported video platforms and the hosts they are recognized by.
 */
public enum Platform {

    YOUTUBE("youtube.com", "youtu.be"),
    TIKTOK("tiktok.com"),
    INSTAGRAM("instagram.com");

    private final List<String> domains;

    Platform(String... domains) {
        this.domains = List.of(domains);
    }

    /**
     * @param host lower-case host name, without a port
     */
    static Optional<Platform> byHost(String host) {
        for (Platform platform : values()) {
            if (platform.matches(host)) {
                return Optional.of(platform);
            }
        }
        return Optional.empty();
    }

    private boolean matches(String host) {
        return domains.stream()
                .anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
    }
}
