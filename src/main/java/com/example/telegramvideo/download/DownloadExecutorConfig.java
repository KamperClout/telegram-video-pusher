package com.example.telegramvideo.download;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DownloadExecutorConfig {

    /**
     * Downloads run here so that a slow download never blocks the Telegram update loop.
     * The queue is bounded on purpose: when it is full the caller is told to try later
     * instead of waiting silently.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService downloadExecutor(VideoDownloadProperties properties) {
        AtomicInteger threadNumber = new AtomicInteger(1);

        return new ThreadPoolExecutor(
                properties.poolSize(),
                properties.poolSize(),
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(properties.queueSize()),
                runnable -> new Thread(runnable, "download-" + threadNumber.getAndIncrement()),
                new ThreadPoolExecutor.AbortPolicy());
    }
}
