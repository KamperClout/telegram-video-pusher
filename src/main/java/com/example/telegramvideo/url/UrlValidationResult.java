package com.example.telegramvideo.url;

/**
 * Result of validating a URL sent by a user.
 * {@code platform} is set only when the status is {@link Status#VALID}.
 */
public record UrlValidationResult(Status status, Platform platform) {

    public enum Status {
        VALID,
        INVALID_URL,
        UNSUPPORTED_PLATFORM
    }

    public static UrlValidationResult valid(Platform platform) {
        return new UrlValidationResult(Status.VALID, platform);
    }

    public static UrlValidationResult invalidUrl() {
        return new UrlValidationResult(Status.INVALID_URL, null);
    }

    public static UrlValidationResult unsupportedPlatform() {
        return new UrlValidationResult(Status.UNSUPPORTED_PLATFORM, null);
    }

    public boolean isValid() {
        return status == Status.VALID;
    }
}
