package com.example.telegramvideo.bot;

import com.example.telegramvideo.download.DownloadedVideo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Sends downloaded videos to Telegram.
 */
@Service
public class TelegramVideoService {

    private static final Logger log = LoggerFactory.getLogger(TelegramVideoService.class);

    private final TelegramClient telegramClient;

    public TelegramVideoService(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    /**
     * @throws TelegramSendException if Telegram rejected the video or was unreachable
     */
    public void sendVideo(Long chatId, DownloadedVideo video) {
        SendVideo sendVideo = new SendVideo(chatId.toString(), new InputFile(video.file().toFile()));
        sendVideo.setSupportsStreaming(true);

        try {
            telegramClient.execute(sendVideo);
            log.info("Sent video {} to chat {}", video.downloadId(), chatId);
        } catch (TelegramApiException e) {
            throw new TelegramSendException(
                    "Failed to send video %s to chat %d".formatted(video.downloadId(), chatId), e);
        }
    }
}
