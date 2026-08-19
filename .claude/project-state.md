# Project State

## Status

Telegram bot answers `/start`. URL validation and the yt-dlp downloader are implemented.
The image is built and the bot runs in Docker: it connects to the Telegram API and
answers `/start`. Video sending is implemented but not wired into the bot yet. yt-dlp 2026.07.04 and FFmpeg 8.0.1 are present in the image.
A real download has not been executed yet: the downloader is not wired into the bot.

## Implemented

* Maven project (`pom.xml`), Spring Boot 3.3.5 parent, Java 21.
* Dependencies: `spring-boot-starter`, `spring-boot-starter-test`.
* Main class `com.example.telegramvideo.TelegramVideoApplication`.
* `src/main/resources/application.yml` with the application name.
* Smoke test `TelegramVideoApplicationTests.contextLoads`.
* `.gitignore`, Git repository, remote `origin`
  (https://github.com/KamperClout/telegram-video-pusher).
* Telegram bot `com.example.telegramvideo.bot.TelegramVideoBot`:
  long polling, `/start` answer, a short hint for any other text message.
* Bot token is read from `TELEGRAM_BOT_TOKEN` via `telegram.bot.token`.
* `com.example.telegramvideo.bot.TelegramVideoService`: sends a downloaded file with
  `SendVideo` (`supportsStreaming`), wraps Telegram failures into `TelegramSendException`.
* `TelegramClientConfig`: a single shared `TelegramClient` bean used by the bot and by
  `TelegramVideoService`.
* `com.example.telegramvideo.url`: `UrlValidationService`, `Platform`, `UrlValidationResult`.
  Detects YouTube (incl. `youtu.be`), TikTok, Instagram; distinguishes
  `INVALID_URL` from `UNSUPPORTED_PLATFORM`. Covered by unit tests.

* `Dockerfile` (multi-stage: Maven build, `eclipse-temurin:21-jre` runtime with
  yt-dlp and FFmpeg), `compose.yaml`, `.dockerignore`, `.env.example`.
  Image builds and runs; the bot token is taken from an uncommitted `.env`.
* `com.example.telegramvideo.download`: `VideoDownloadService`, `DownloadedVideo`,
  `VideoDownloadException` (with `Reason`), `VideoDownloadProperties`.
  Runs yt-dlp through `ProcessBuilder` with `--no-playlist`, MP4 preference and a timeout;
  each download uses its own temporary directory and the directory is removed on failure.

`UrlValidationService`, `VideoDownloadService` and `TelegramVideoService` are not wired
into the bot yet: the bot still answers any text with a hint. Cleanup of a successful
download is not implemented yet.

## Architecture

Implemented:

```text
Telegram Bot (TelegramVideoBot)
    ↓
URL Validation (UrlValidationService)     [not wired into the bot yet]
    ↓
Video Download (VideoDownloadService)     [not wired into the bot yet]
    ↓
Video Sending (TelegramVideoService)      [not wired into the bot yet]
```

Planned architecture:

```text
Telegram Bot
    ↓
URL Validation
    ↓
Video Download
    ↓
Telegram Video Sending
```

## Technology

* Java 21
* Spring Boot 3.3.5
* Maven
* Telegram Bot API (org.telegram:telegrambots 10.2.0)
* yt-dlp
* FFmpeg
* Docker

## Important Decisions

* MVP is a monolith.
* No database for the first version.
* No Redis.
* No AI yet.
* yt-dlp will be used for video downloading.
* FFmpeg will be used when media processing is required.
* Temporary files must be deleted after processing.
* Secrets must be provided through environment variables.
* Base package: `com.example.telegramvideo`.
* Plain `spring-boot-starter` is used, not `spring-boot-starter-web`: the MVP has no web application.
* Telegram library: `telegrambots-springboot-longpolling-starter` + `telegrambots-client` 10.2.0
  (the old `telegrambots-spring-boot-starter` is frozen at 6.9.7.1).
* API v10 does not need the bot username, so only `TELEGRAM_BOT_TOKEN` is used.
* `TELEGRAM_BOT_TOKEN` has no default: the application fails fast if it is not set.
* A URL is accepted only with an explicit `http`/`https` scheme; a host matches a platform
  when it equals the platform domain or is a subdomain of it (covers `www`, `m`, `vm`, `vt`).
* Downloader configuration lives under `video.download.*`:
  `YT_DLP_PATH`, `VIDEO_DOWNLOAD_DIR`, `VIDEO_DOWNLOAD_TIMEOUT`, `VIDEO_MAX_FILE_SIZE`.
* On success `VideoDownloadService` keeps the temporary directory and returns it in
  `DownloadedVideo.workDir`: the caller removes it after the video has been sent.
* yt-dlp output is redirected to `yt-dlp.log` inside the temporary directory,
  so the process cannot block on a full pipe; the log is parsed only on failure.
* The application is run through Docker; nothing is installed on the development machine.
* The runtime image creates its own `app` user without a fixed uid:
  `eclipse-temurin:21-jre` already occupies uid 1000.
* Tests exclude `TelegramBotStarterConfiguration` (`src/test/resources/application.yml`),
  so tests never connect to Telegram.

## Known Problems

* yt-dlp and FFmpeg are not installed on the development machine on purpose:
  they are provided by the Docker image. The download path is verified only by
  unit tests so far, never against a real video.
* Maven is not installed system-wide. The build was run with the Maven bundled with IntelliJ IDEA
  (`C:\Program Files\JetBrains\IntelliJ IDEA 2024.2.5\plugins\maven\lib\maven3\bin\mvn.cmd`)
  and `JAVA_HOME` pointed at `C:\Users\Pavel\.jdks\corretto-21.0.7`
  (the default system JDK is 24).

## Current Task

Wire the bot to url validation, downloading and sending; add `FileCleanupService`
and user-facing error messages.

## Next Steps

1. Wire the bot to url validation, downloading and sending; add `FileCleanupService`
   and user-facing error messages.
3. Add concurrent downloads and in-memory rate limiting.
