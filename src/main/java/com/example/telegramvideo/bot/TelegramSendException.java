package com.example.telegramvideo.bot;

/**
 * A call to the Telegram API failed.
 */
public class TelegramSendException extends RuntimeException {

    public TelegramSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
