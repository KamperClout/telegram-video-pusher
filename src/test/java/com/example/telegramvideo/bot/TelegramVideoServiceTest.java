package com.example.telegramvideo.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.telegramvideo.download.DownloadedVideo;
import com.example.telegramvideo.url.Platform;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

class TelegramVideoServiceTest {

    @TempDir
    Path workDir;

    private final TelegramClient telegramClient = mock(TelegramClient.class);
    private final TelegramVideoService service = new TelegramVideoService(telegramClient);

    private DownloadedVideo video;

    @BeforeEach
    void createVideo() throws IOException {
        Path file = Files.writeString(workDir.resolve("video.mp4"), "not a real video");
        video = new DownloadedVideo("download-1", Platform.YOUTUBE, file, workDir, Files.size(file));
    }

    @Test
    void sendsVideoFileToTheChat() throws TelegramApiException {
        service.sendVideo(42L, video);

        ArgumentCaptor<SendVideo> captor = ArgumentCaptor.forClass(SendVideo.class);
        verify(telegramClient).execute(captor.capture());

        SendVideo sent = captor.getValue();
        assertThat(sent.getChatId()).isEqualTo("42");
        assertThat(sent.getVideo().getNewMediaFile()).isEqualTo(video.file().toFile());
        assertThat(sent.getSupportsStreaming()).isTrue();
    }

    @Test
    void wrapsTelegramFailures() throws TelegramApiException {
        when(telegramClient.execute(any(SendVideo.class))).thenThrow(new TelegramApiException("boom"));

        assertThatThrownBy(() -> service.sendVideo(42L, video))
                .isInstanceOf(TelegramSendException.class)
                .hasMessageContaining("download-1")
                .hasCauseInstanceOf(TelegramApiException.class);
    }
}
