package com.example.telegramvideo.url;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * URL validation and platform detection.
 */
@Service
public class UrlValidationService {

    public UrlValidationResult validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return UrlValidationResult.invalidUrl();
        }

        URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (URISyntaxException e) {
            return UrlValidationResult.invalidUrl();
        }

        if (!isHttpScheme(uri.getScheme()) || uri.getHost() == null) {
            return UrlValidationResult.invalidUrl();
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        Optional<Platform> platform = Platform.byHost(host);

        return platform.map(UrlValidationResult::valid)
                .orElseGet(UrlValidationResult::unsupportedPlatform);
    }

    private boolean isHttpScheme(String scheme) {
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }
}
