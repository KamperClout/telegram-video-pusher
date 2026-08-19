package com.example.telegramvideo.request;

import com.example.telegramvideo.bot.TelegramMessageService;
import com.example.telegramvideo.bot.TelegramSendException;
import com.example.telegramvideo.bot.TelegramVideoService;
import com.example.telegramvideo.download.DownloadedVideo;
import com.example.telegramvideo.download.FileCleanupService;
import com.example.telegramvideo.download.VideoDownloadException;
import com.example.telegramvideo.download.VideoDownloadService;
import com.example.telegramvideo.url.UrlValidationResult;
import com.example.telegramvideo.url.UrlValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Handles a video request end to end: validate, download, send, clean up.
 */
@Service
public class VideoRequestService {

    private static final Logger log = LoggerFactory.getLogger(VideoRequestService.class);

    static final String INVALID_URL_MESSAGE =
            "Это не похоже на ссылку. Пришли ссылку на видео из YouTube, TikTok или Instagram.";
    static final String UNSUPPORTED_PLATFORM_MESSAGE =
            "Я умею скачивать только из YouTube, TikTok и Instagram.";
    static final String DOWNLOAD_STARTED_MESSAGE = "Скачиваю видео, это займёт немного времени…";
    static final String PRIVATE_VIDEO_MESSAGE = "Это видео приватное, скачать его не получится.";
    static final String VIDEO_UNAVAILABLE_MESSAGE = "Видео недоступно: возможно, оно удалено или скрыто.";
    static final String TIMEOUT_MESSAGE = "Скачивание заняло слишком много времени. Попробуй видео покороче.";
    static final String FILE_TOO_LARGE_MESSAGE = "Видео слишком большое, Telegram не пропустит такой файл.";
    static final String DOWNLOAD_FAILED_MESSAGE = "Не удалось скачать видео. Попробуй ещё раз позже.";
    static final String SEND_FAILED_MESSAGE = "Видео скачалось, но отправить его не получилось. Попробуй ещё раз.";

    private final UrlValidationService urlValidationService;
    private final VideoDownloadService videoDownloadService;
    private final TelegramVideoService telegramVideoService;
    private final TelegramMessageService telegramMessageService;
    private final FileCleanupService fileCleanupService;

    public VideoRequestService(UrlValidationService urlValidationService,
                               VideoDownloadService videoDownloadService,
                               TelegramVideoService telegramVideoService,
                               TelegramMessageService telegramMessageService,
                               FileCleanupService fileCleanupService) {
        this.urlValidationService = urlValidationService;
        this.videoDownloadService = videoDownloadService;
        this.telegramVideoService = telegramVideoService;
        this.telegramMessageService = telegramMessageService;
        this.fileCleanupService = fileCleanupService;
    }

    public void handle(Long chatId, String text) {
        UrlValidationResult validation = urlValidationService.validate(text);

        switch (validation.status()) {
            case INVALID_URL -> telegramMessageService.sendText(chatId, INVALID_URL_MESSAGE);
            case UNSUPPORTED_PLATFORM -> telegramMessageService.sendText(chatId, UNSUPPORTED_PLATFORM_MESSAGE);
            case VALID -> downloadAndSend(chatId, text.trim(), validation);
        }
    }

    private void downloadAndSend(Long chatId, String url, UrlValidationResult validation) {
        telegramMessageService.sendText(chatId, DOWNLOAD_STARTED_MESSAGE);

        DownloadedVideo video = null;
        try {
            video = videoDownloadService.download(url, validation.platform());
            telegramVideoService.sendVideo(chatId, video);
        } catch (VideoDownloadException e) {
            log.warn("Download failed for chat {}: {}", chatId, e.getMessage(), e);
            telegramMessageService.sendText(chatId, messageFor(e));
        } catch (TelegramSendException e) {
            log.error("Sending failed for chat {}", chatId, e);
            telegramMessageService.sendText(chatId, SEND_FAILED_MESSAGE);
        } finally {
            if (video != null) {
                fileCleanupService.deleteDirectory(video.workDir());
            }
        }
    }

    private String messageFor(VideoDownloadException e) {
        return switch (e.reason()) {
            case PRIVATE_VIDEO -> PRIVATE_VIDEO_MESSAGE;
            case VIDEO_UNAVAILABLE -> VIDEO_UNAVAILABLE_MESSAGE;
            case TIMEOUT -> TIMEOUT_MESSAGE;
            case FILE_TOO_LARGE -> FILE_TOO_LARGE_MESSAGE;
            case FILE_MISSING, DOWNLOAD_FAILED -> DOWNLOAD_FAILED_MESSAGE;
        };
    }
}
