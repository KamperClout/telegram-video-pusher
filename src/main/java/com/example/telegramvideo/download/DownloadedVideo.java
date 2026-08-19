package com.example.telegramvideo.download;

import com.example.telegramvideo.url.Platform;
import java.nio.file.Path;

/**
 * A successfully downloaded video.
 *
 * @param downloadId  identifier of the download, also the name of its temporary directory
 * @param platform    platform the video was taken from
 * @param file        downloaded file
 * @param workDir     temporary directory holding the file; must be removed after use
 * @param fileSize    file size in bytes
 */
public record DownloadedVideo(
        String downloadId,
        Platform platform,
        Path file,
        Path workDir,
        long fileSize) {
}
