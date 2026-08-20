package com.example.telegramvideo.download;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param ytDlpPath  yt-dlp executable (name on PATH or absolute path)
 * @param downloadDir root directory for per-download temporary directories
 * @param timeout    maximum duration of a single yt-dlp run
 * @param maxFileSize maximum size of a downloaded file, in bytes
 * @param heights     video heights to try, from best to worst: when the result does not fit
 *                    into {@code maxFileSize}, the next height is tried
 * @param poolSize    how many downloads may run in parallel
 * @param queueSize   how many downloads may wait for a free slot
 */
@ConfigurationProperties(prefix = "video.download")
public record VideoDownloadProperties(
        String ytDlpPath,
        String downloadDir,
        Duration timeout,
        long maxFileSize,
        List<Integer> heights,
        int poolSize,
        int queueSize) {
}
