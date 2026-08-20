package com.example.telegramvideo.download;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Removes temporary files of a download.
 */
@Service
public class FileCleanupService {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupService.class);

    /**
     * Deletes everything inside a directory but keeps the directory itself.
     * Used between download attempts, so a retry starts from an empty directory.
     */
    public void clearDirectory(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.list(directory)) {
            paths.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException | UncheckedIOException e) {
            log.warn("Failed to clear temporary directory {}", directory, e);
        }
    }

    /**
     * Deletes a directory with everything inside it. Never throws: cleanup failures
     * must not break the user flow, they are only logged.
     */
    public void deleteDirectory(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            log.debug("Deleted temporary directory {}", directory);
        } catch (IOException | UncheckedIOException e) {
            log.warn("Failed to delete temporary directory {}", directory, e);
        }
    }
}
