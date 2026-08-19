# --- build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies are cached separately from the sources.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# --- runtime ---
FROM eclipse-temurin:21-jre

# The nightly channel is used on purpose: platform fixes (TikTok, YouTube) land there
# weeks before a stable release, and stale yt-dlp means broken downloads.
# yt-dlp needs python3, FFmpeg is used to merge video and audio into MP4.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg python3 curl ca-certificates \
    && curl -sSL https://github.com/yt-dlp/yt-dlp-nightly-builds/releases/latest/download/yt-dlp \
       -o /usr/local/bin/yt-dlp \
    && chmod +x /usr/local/bin/yt-dlp \
    && rm -rf /var/lib/apt/lists/*

# YouTube hides some formats behind JavaScript; yt-dlp needs a JS runtime to read them.
COPY --from=denoland/deno:bin /deno /usr/local/bin/deno
RUN chmod +x /usr/local/bin/deno

# The base image already uses uid 1000, so let useradd pick a free one.
RUN useradd --create-home app
USER app

WORKDIR /app
COPY --from=build /build/target/telegram-video-*.jar app.jar

ENV VIDEO_DOWNLOAD_DIR=/tmp/telegram-video

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
