# Telegram Video Bot

## Project

Telegram bot for downloading videos from YouTube, TikTok and Instagram.

Main user flow:

```text
User sends URL
↓
Detect platform
↓
Download video with yt-dlp
↓
Send video to Telegram
↓
Delete temporary files
```

This is an MVP. Keep the implementation simple.

---

## Technology Stack

* Java 21
* Spring Boot 3.x
* Maven
* Telegram Bot API
* yt-dlp
* FFmpeg
* Docker / Docker Compose

---

## Architecture

Use a simple monolithic application.

```text
Telegram Bot
    ↓
URL Validation
    ↓
Video Download
    ↓
Telegram Video Sending
```

Main responsibilities:

* Telegram Bot — Telegram interaction only.
* UrlValidationService — URL validation and platform detection.
* VideoDownloadService — download videos using yt-dlp.
* TelegramVideoService — send downloaded videos to Telegram.
* FileCleanupService — remove temporary files.

Do not put business logic directly inside the Telegram bot class.

---

## MVP Scope

Implement only:

* `/start`
* receiving URLs
* YouTube support
* TikTok support
* Instagram support
* video downloading
* sending videos back to Telegram
* error handling
* temporary file cleanup
* basic concurrent downloads
* basic in-memory rate limiting

---

## Do NOT Implement Yet

Do not add these unless explicitly requested:

* PostgreSQL
* Redis
* Kafka
* RabbitMQ
* microservices
* Kubernetes
* frontend
* web application
* authentication
* payments
* subscriptions
* admin panel
* AI
* LLM
* transcription
* subtitles
* video editing
* automatic clips

Do not overengineer the MVP.

---

## Downloader

Use `yt-dlp` for downloading.

Do not implement custom scraping for YouTube, TikTok or Instagram.

Run yt-dlp through Java `ProcessBuilder`.

Never concatenate user input into shell commands.

Bad:

```java
Runtime.getRuntime().exec("yt-dlp " + url);
```

Always pass the URL as a separate ProcessBuilder argument.

Do not download playlists.

Prefer MP4 output and use FFmpeg when necessary.

Each download must use its own temporary directory.

Always clean temporary files in `finally`.

---

## Security

Never store secrets in source code.

Telegram credentials must come from environment variables.

Never log the Telegram bot token.

Never execute arbitrary commands constructed from user input.

Validate supported URL hosts before downloading.

---

## Configuration

Important configuration must be externalized.

Use environment variables for secrets and deployment-specific settings.

Examples:

```text
TELEGRAM_BOT_USERNAME
TELEGRAM_BOT_TOKEN
VIDEO_DOWNLOAD_DIR
VIDEO_DOWNLOAD_TIMEOUT
VIDEO_MAX_FILE_SIZE
VIDEO_DOWNLOAD_POOL_SIZE
RATE_LIMIT_MAX_REQUESTS_PER_MINUTE
```

Never hardcode secrets.

---

## Error Handling

Users should receive simple human-readable error messages.

Never expose stack traces to users.

Detailed technical errors should be written to logs.

Handle at least:

* invalid URL
* unsupported platform
* unavailable video
* private video
* deleted video
* yt-dlp failure
* download timeout
* missing file
* file too large
* Telegram API failure

---

## Code Quality

Prefer:

* simple classes
* clear responsibilities
* dependency injection
* readable code
* KISS
* SOLID where useful

Avoid:

* unnecessary abstractions
* unnecessary interfaces
* premature optimization
* overengineering
* unrelated refactoring

Do not introduce design patterns unless they solve a real problem.

---

## Testing

Add unit tests for important business logic.

Priorities:

1. URL validation
2. platform detection
3. rate limiting
4. error handling

Do not create excessive tests for trivial code.

Do not test real Telegram API calls unless explicitly requested.

---

## Development Workflow

Work in small iterations.

Before implementing a task:

1. Read `CLAUDE.md`.
2. Read `.claude/project-state.md` if it exists.
3. Inspect only the relevant existing code.
4. Determine the minimum required changes.
5. Implement only the requested task.

After implementation:

1. Run `mvn test`.
2. Fix errors related to the current task.
3. Check that existing functionality still works.
4. Update `.claude/project-state.md` when the project state changes.
5. Do not perform unrelated refactoring.

---

## Project State

`.claude/project-state.md` is the short source of truth for the current implementation state.

Keep it concise and factual.

After completing a meaningful development task, update it with:

* implemented features
* current architecture
* important technical decisions
* known problems
* current task
* next logical steps

Do not put conversation history into this file.

Do not claim that something is implemented if it is not actually implemented.

---

## Memory

Use Claude Code Auto Memory for useful long-term development knowledge.

Examples:

* recurring debugging discoveries
* library-specific quirks
* environment-specific issues
* lessons learned
* useful development commands

Do not duplicate the entire project state in Auto Memory.

---

## Git

Use Git from the beginning.

Prefer small meaningful commits after completed milestones.

Example commit names:

```text
feat: initialize Spring Boot project
feat: add Telegram bot
feat: add URL validation
feat: add video downloader
feat: send downloaded videos
```

Do not rewrite Git history unless explicitly requested.

---

## Future AI

AI will be added later.

Possible future pipeline:

```text
Video
↓
Audio extraction
↓
Speech-to-text
↓
LLM
↓
Summary / Q&A / Clips
```

Do not implement AI now.

The downloader should return a reusable result containing information such as:

* download ID
* platform
* file path
* file size
* duration if available

This is only a future architectural consideration. Do not create unnecessary AI-related code now.

---

## Important Rule

Do exactly the requested task.

Do not automatically implement future features.

If you notice a useful improvement:

1. Do not implement it automatically.
2. Mention it after completing the requested task.

Keep the project small, understandable and maintainable.
