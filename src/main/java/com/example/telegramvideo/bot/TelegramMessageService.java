package com.example.telegramvideo.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Sends text messages to Telegram.
 */
@Service
public class TelegramMessageService {

    private static final Logger log = LoggerFactory.getLogger(TelegramMessageService.class);

    private final TelegramClient telegramClient;

    public TelegramMessageService(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    /**
     * Never throws: a failed reply must not break the flow, it is only logged.
     */
    public void sendText(Long chatId, String text) {
        try {
            telegramClient.execute(new SendMessage(chatId.toString(), text));
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}", chatId, e);
        }
    }
}
