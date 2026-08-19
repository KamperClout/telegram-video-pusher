package com.example.telegramvideo.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.telegramvideo.download.VideoDownloadException.Reason;
import com.example.telegramvideo.url.Platform;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VideoDownloadServiceTest {

    @TempDir
    Path downloadDir;

    @ParameterizedTest
    @ValueSource(strings = {
            "ERROR: [youtube] abc: Private video. Sign in if you've been granted access to this video",
            "ERROR: [instagram] Requested content is not available, login required"
    })
    void classifiesPrivateVideo(String output) {
        assertThat(VideoDownloadService.classify(output)).isEqualTo(Reason.PRIVATE_VIDEO);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ERROR: [youtube] abc: Video unavailable",
            "ERROR: [youtube] abc: This video has been removed by the uploader",
            "ERROR: [tiktok] 123: Video not available"
    })
    void classifiesUnavailableVideo(String output) {
        assertThat(VideoDownloadService.classify(output)).isEqualTo(Reason.VIDEO_UNAVAILABLE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ERROR: unable to download video data: HTTP Error 500",
            ""
    })
    void classifiesEverythingElseAsDownloadFailure(String output) {
        assertThat(VideoDownloadService.classify(output)).isEqualTo(Reason.DOWNLOAD_FAILED);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "File is larger than max-filesize (91234567 > 52428800 bytes), skipping",
            "ERROR: File is larger than max-filesize"
    })
    void recognizesTheMaxFilesizeSkip(String output) {
        assertThat(VideoDownloadService.tooLargeReported(output)).isTrue();
    }

    @Test
    void doesNotSeeAMaxFilesizeSkipInOrdinaryOutput() {
        assertThat(VideoDownloadService.tooLargeReported("[download] Download completed")).isFalse();
    }

    @Test
    void failsAndCleansUpWhenYtDlpCannotBeStarted() throws IOException {
        VideoDownloadService service = serviceWith("yt-dlp-does-not-exist");

        assertThatThrownBy(() -> service.download("https://youtu.be/dQw4w9WgXcQ", Platform.YOUTUBE))
                .isInstanceOf(VideoDownloadException.class)
                .extracting(e -> ((VideoDownloadException) e).reason())
                .isEqualTo(Reason.DOWNLOAD_FAILED);

        assertThat(workDirs()).isEmpty();
    }

    private VideoDownloadService serviceWith(String ytDlpPath) {
        return new VideoDownloadService(
                new VideoDownloadProperties(ytDlpPath, downloadDir.toString(), Duration.ofSeconds(5),
                        50L * 1024 * 1024, 1080, 2, 10),
                new FileCleanupService());
    }

    private List<Path> workDirs() throws IOException {
        try (var files = Files.list(downloadDir)) {
            return files.toList();
        }
    }
}
