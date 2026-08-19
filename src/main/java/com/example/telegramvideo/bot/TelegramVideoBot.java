package com.example.telegramvideo.bot;

import com.example.telegramvideo.request.VideoRequestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Telegram interaction only. Business logic lives in the services.
 */
@Component
public class TelegramVideoBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final String START_MESSAGE =
            "Привет! Пришли ссылку на видео из YouTube, TikTok или Instagram — я скачаю его и отправлю сюда.";

    private final String botToken;
    private final TelegramMessageService telegramMessageService;
    private final VideoRequestService videoRequestService;

    public TelegramVideoBot(@Value("${telegram.bot.token}") String botToken,
                            TelegramMessageService telegramMessageService,
                            VideoRequestService videoRequestService) {
        this.botToken = botToken;
        this.telegramMessageService = telegramMessageService;
        this.videoRequestService = videoRequestService;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        if ("/start".equals(text)) {
            telegramMessageService.sendText(chatId, START_MESSAGE);
        } else {
            videoRequestService.handle(chatId, text);
        }
    }
}
