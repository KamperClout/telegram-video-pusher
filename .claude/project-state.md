# Project State

## Status

Spring Boot skeleton created. Build and tests pass.

## Implemented

* Maven project (`pom.xml`), Spring Boot 3.3.5 parent, Java 21.
* Dependencies: `spring-boot-starter`, `spring-boot-starter-test`.
* Main class `com.example.telegramvideo.TelegramVideoApplication`.
* `src/main/resources/application.yml` with the application name.
* Smoke test `TelegramVideoApplicationTests.contextLoads`.
* `.gitignore`.

Business logic is not implemented yet.

## Architecture

Not implemented yet.

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
* Telegram Bot API
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

## Known Problems

* Maven is not installed system-wide. The build was run with the Maven bundled with IntelliJ IDEA
  (`C:\Program Files\JetBrains\IntelliJ IDEA 2024.2.5\plugins\maven\lib\maven3\bin\mvn.cmd`)
  and `JAVA_HOME` pointed at `C:\Users\Pavel\.jdks\corretto-21.0.7`
  (the default system JDK is 24).

## Current Task

Configure the Telegram bot.

## Next Steps

1. Configure the Telegram bot (env-based credentials, `/start`).
2. Implement URL validation and platform detection.
3. Implement the yt-dlp downloader.
4. Send downloaded videos back to Telegram.
5. Add Docker support.
