package com.example.telegramvideo.download;

import com.example.telegramvideo.download.VideoDownloadException.Reason;
import com.example.telegramvideo.url.Platform;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Downloads videos with yt-dlp. Each download gets its own temporary directory.
 */
@Service
public class VideoDownloadService {

    private static final Logger log = LoggerFactory.getLogger(VideoDownloadService.class);

    private static final String LOG_FILE_NAME = "yt-dlp.log";
    private static final String OUTPUT_TEMPLATE = "video.%(ext)s";

    private final VideoDownloadProperties properties;
    private final FileCleanupService fileCleanupService;

    public VideoDownloadService(VideoDownloadProperties properties, FileCleanupService fileCleanupService) {
        this.properties = properties;
        this.fileCleanupService = fileCleanupService;
    }

    /**
     * @throws VideoDownloadException if the video could not be downloaded;
     *                                the temporary directory is removed in that case
     */
    public DownloadedVideo download(String url, Platform platform) {
        String downloadId = UUID.randomUUID().toString();
        Path workDir = createWorkDir(downloadId);

        try {
            runYtDlp(url, workDir);

            Path file = findDownloadedFile(workDir);
            long fileSize = Files.size(file);
            if (fileSize > properties.maxFileSize()) {
                throw new VideoDownloadException(Reason.FILE_TOO_LARGE,
                        "Downloaded file is %d bytes, limit is %d".formatted(fileSize, properties.maxFileSize()));
            }

            log.info("Downloaded {} video, id={}, size={} bytes", platform, downloadId, fileSize);
            return new DownloadedVideo(downloadId, platform, file, workDir, fileSize);
        } catch (IOException e) {
            fileCleanupService.deleteDirectory(workDir);
            throw new VideoDownloadException(Reason.DOWNLOAD_FAILED, "Failed to read the downloaded file", e);
        } catch (RuntimeException e) {
            fileCleanupService.deleteDirectory(workDir);
            throw e;
        }
    }

    private Path createWorkDir(String downloadId) {
        try {
            Path root = Path.of(properties.downloadDir());
            Files.createDirectories(root);
            return Files.createDirectory(root.resolve(downloadId));
        } catch (IOException e) {
            throw new VideoDownloadException(Reason.DOWNLOAD_FAILED, "Failed to create a temporary directory", e);
        }
    }

    private void runYtDlp(String url, Path workDir) {
        // The URL is always passed as a separate argument, never concatenated into a command line.
        ProcessBuilder processBuilder = new ProcessBuilder(
                properties.ytDlpPath(),
                "--no-playlist",
                "--no-progress",
                "--restrict-filenames",
                "--format", "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/b",
                "--merge-output-format", "mp4",
                "--output", OUTPUT_TEMPLATE,
                url);
        processBuilder.directory(workDir.toFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(workDir.resolve(LOG_FILE_NAME).toFile());

        Process process = null;
        try {
            process = processBuilder.start();
            boolean finished = process.waitFor(properties.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new VideoDownloadException(Reason.TIMEOUT, "yt-dlp timed out after " + properties.timeout());
            }
            if (process.exitValue() != 0) {
                String output = readLog(workDir);
                log.warn("yt-dlp exited with code {}: {}", process.exitValue(), output);
                throw new VideoDownloadException(classify(output), "yt-dlp exited with code " + process.exitValue());
            }
        } catch (IOException e) {
            throw new VideoDownloadException(Reason.DOWNLOAD_FAILED, "Failed to start yt-dlp", e);
        } catch (InterruptedException e) {
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            throw new VideoDownloadException(Reason.DOWNLOAD_FAILED, "Download was interrupted", e);
        }
    }

    private Path findDownloadedFile(Path workDir) {
        try (Stream<Path> files = Files.list(workDir)) {
            Optional<Path> file = files
                    .filter(Files::isRegularFile)
                    .filter(path -> !LOG_FILE_NAME.equals(path.getFileName().toString()))
                    .findFirst();
            return file.orElseThrow(() -> new VideoDownloadException(Reason.FILE_MISSING,
                    "yt-dlp produced no file in " + workDir));
        } catch (IOException e) {
            throw new VideoDownloadException(Reason.FILE_MISSING, "Failed to list " + workDir, e);
        }
    }

    private String readLog(Path workDir) {
        Path logFile = workDir.resolve(LOG_FILE_NAME);
        try {
            return Files.exists(logFile) ? Files.readString(logFile, StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            log.warn("Failed to read {}", logFile, e);
            return "";
        }
    }

    /**
     * Maps yt-dlp output to a reason. Package-private for tests.
     */
    static Reason classify(String output) {
        String text = output.toLowerCase(Locale.ROOT);

        List<String> privateMarkers = List.of("private video", "is private", "login required",
                "sign in to confirm", "requires authentication");
        if (privateMarkers.stream().anyMatch(text::contains)) {
            return Reason.PRIVATE_VIDEO;
        }

        List<String> unavailableMarkers = List.of("video unavailable", "has been removed", "no longer available",
                "not available", "does not exist", "removed by the uploader", "content isn't available");
        if (unavailableMarkers.stream().anyMatch(text::contains)) {
            return Reason.VIDEO_UNAVAILABLE;
        }

        return Reason.DOWNLOAD_FAILED;
    }
}
