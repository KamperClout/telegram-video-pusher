package com.example.telegramvideo.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Telegram interaction only. Business logic is handled by separate services.
 */
@Component
public class TelegramVideoBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelegramVideoBot.class);

    private static final String START_MESSAGE =
            "Привет! Пришли ссылку на видео из YouTube, TikTok или Instagram — я скачаю его и отправлю сюда.";

    private static final String HELP_MESSAGE =
            "Пришли ссылку на видео из YouTube, TikTok или Instagram.";

    private final String botToken;
    private final TelegramClient telegramClient;

    public TelegramVideoBot(@Value("${telegram.bot.token}") String botToken, TelegramClient telegramClient) {
        this.botToken = botToken;
        this.telegramClient = telegramClient;
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
            sendText(chatId, START_MESSAGE);
        } else {
            sendText(chatId, HELP_MESSAGE);
        }
    }

    private void sendText(Long chatId, String text) {
        try {
            telegramClient.execute(new SendMessage(chatId.toString(), text));
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}", chatId, e);
        }
    }
}
