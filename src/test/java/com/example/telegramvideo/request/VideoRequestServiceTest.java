package com.example.telegramvideo.request;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.telegramvideo.bot.TelegramMessageService;
import com.example.telegramvideo.bot.TelegramSendException;
import com.example.telegramvideo.bot.TelegramVideoService;
import com.example.telegramvideo.download.DownloadedVideo;
import com.example.telegramvideo.download.FileCleanupService;
import com.example.telegramvideo.download.VideoDownloadException;
import com.example.telegramvideo.download.VideoDownloadException.Reason;
import com.example.telegramvideo.download.VideoDownloadService;
import com.example.telegramvideo.ratelimit.RateLimitService;
import com.example.telegramvideo.url.Platform;
import com.example.telegramvideo.url.UrlValidationResult;
import com.example.telegramvideo.url.UrlValidationService;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

class VideoRequestServiceTest {

    private static final Long CHAT_ID = 42L;
    private static final String URL = "https://youtu.be/dQw4w9WgXcQ";

    private final UrlValidationService urlValidationService = mock(UrlValidationService.class);
    private final VideoDownloadService videoDownloadService = mock(VideoDownloadService.class);
    private final TelegramVideoService telegramVideoService = mock(TelegramVideoService.class);
    private final TelegramMessageService telegramMessageService = mock(TelegramMessageService.class);
    private final FileCleanupService fileCleanupService = mock(FileCleanupService.class);
    private final RateLimitService rateLimitService = mock(RateLimitService.class);

    /** Runs submitted downloads on the calling thread, so the tests stay deterministic. */
    private final ExecutorService downloadExecutor = mock(ExecutorService.class);

    private final VideoRequestService service = new VideoRequestService(
            urlValidationService, videoDownloadService, telegramVideoService,
            telegramMessageService, fileCleanupService, rateLimitService, downloadExecutor);

    @org.junit.jupiter.api.BeforeEach
    void allowRequestsAndRunInline() {
        when(rateLimitService.tryAcquire(any())).thenReturn(true);
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(downloadExecutor).execute(any(Runnable.class));
    }

    @Test
    void answersWhenTheChatIsOverTheRateLimit() {
        when(rateLimitService.tryAcquire(CHAT_ID)).thenReturn(false);

        service.handle(CHAT_ID, URL);

        verify(telegramMessageService).sendText(CHAT_ID, VideoRequestService.RATE_LIMITED_MESSAGE);
        verifyNoInteractions(urlValidationService, videoDownloadService, telegramVideoService);
    }

    @Test
    void answersWhenTheDownloadQueueIsFull() {
        acceptUrl();
        org.mockito.Mockito.doThrow(new RejectedExecutionException())
                .when(downloadExecutor).execute(any(Runnable.class));

        service.handle(CHAT_ID, URL);

        verify(telegramMessageService).sendText(CHAT_ID, VideoRequestService.BUSY_MESSAGE);
        verify(telegramMessageService, never()).sendText(CHAT_ID, VideoRequestService.DOWNLOAD_STARTED_MESSAGE);
        verifyNoInteractions(videoDownloadService, telegramVideoService);
    }

    private final DownloadedVideo downloadedVideo = new DownloadedVideo(
            "download-1", Platform.YOUTUBE, Path.of("work", "video.mp4"), Path.of("work"), 1024L);

    @Test
    void answersOnInvalidUrl() {
        when(urlValidationService.validate(anyString())).thenReturn(UrlValidationResult.invalidUrl());

        service.handle(CHAT_ID, "hello");

        verify(telegramMessageService).sendText(CHAT_ID, VideoRequestService.INVALID_URL_MESSAGE);
        verifyNoInteractions(videoDownloadService, telegramVideoService, fileCleanupService);
    }

    @Test
    void answersOnUnsupportedPlatform() {
        when(urlValidationService.validate(anyString())).thenReturn(UrlValidationResult.unsupportedPlatform());

        service.handle(CHAT_ID, "https://vimeo.com/1");

        verify(telegramMessageService).sendText(CHAT_ID, VideoRequestService.UNSUPPORTED_PLATFORM_MESSAGE);
        verifyNoInteractions(videoDownloadService, telegramVideoService, fileCleanupService);
    }

    @Test
    void downloadsSendsAndCleansUp() {
        acceptUrl();
        when(videoDownloadService.download(URL, Platform.YOUTUBE)).thenReturn(downloadedVideo);

        service.handle(CHAT_ID, URL);

        verify(telegramMessageService).sendText(CHAT_ID, VideoRequestService.DOWNLOAD_STARTED_MESSAGE);
        verify(telegramVideoService).sendVideo(CHAT_ID, downloadedVideo);
        verify(fileCleanupService).deleteDirectory(downloadedVideo.workDir());
    }

    @ParameterizedTest
    @CsvSource({
            "PRIVATE_VIDEO, PRIVATE_VIDEO_MESSAGE",
            "VIDEO_UNAVAILABLE, VIDEO_UNAVAILABLE_MESSAGE",
            "TIMEOUT, TIMEOUT_MESSAGE",
            "FILE_TOO_LARGE, FILE_TOO_LARGE_MESSAGE",
            "FILE_MISSING, DOWNLOAD_FAILED_MESSAGE",
            "DOWNLOAD_FAILED, DOWNLOAD_FAILED_MESSAGE"
    })
    void reportsDownloadFailures(Reason reason, String expectedMessageName) {
        acceptUrl();
        when(videoDownloadService.download(anyString(), any()))
                .thenThrow(new VideoDownloadException(reason, "failed"));

        service.handle(CHAT_ID, URL);

        verify(telegramMessageService).sendText(CHAT_ID, messageByName(expectedMessageName));
        verify(telegramVideoService, never()).sendVideo(any(), any());
        verify(fileCleanupService, never()).deleteDirectory(any());
    }

    @Test
    void reportsSendFailureAndStillCleansUp() {
        acceptUrl();
        when(videoDownloadService.download(URL, Platform.YOUTUBE)).thenReturn(downloadedVideo);
        org.mockito.Mockito.doThrow(new TelegramSendException("boom", new TelegramApiException("boom")))
                .when(telegramVideoService).sendVideo(eq(CHAT_ID), any());

        service.handle(CHAT_ID, URL);

        verify(telegramMessageService).sendText(CHAT_ID, VideoRequestService.SEND_FAILED_MESSAGE);
        verify(fileCleanupService).deleteDirectory(downloadedVideo.workDir());
    }

    private void acceptUrl() {
        when(urlValidationService.validate(anyString()))
                .thenReturn(UrlValidationResult.valid(Platform.YOUTUBE));
    }

    private String messageByName(String name) {
        try {
            return (String) VideoRequestService.class.getDeclaredField(name).get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
