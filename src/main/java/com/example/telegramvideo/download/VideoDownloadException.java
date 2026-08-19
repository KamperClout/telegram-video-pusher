package com.example.telegramvideo.download;

/**
 * Download failure with a reason the caller can turn into a user-facing message.
 */
public class VideoDownloadException extends RuntimeException {

    public enum Reason {
        PRIVATE_VIDEO,
        VIDEO_UNAVAILABLE,
        TIMEOUT,
        FILE_TOO_LARGE,
        FILE_MISSING,
        DOWNLOAD_FAILED
    }

    private final Reason reason;

    public VideoDownloadException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public VideoDownloadException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
