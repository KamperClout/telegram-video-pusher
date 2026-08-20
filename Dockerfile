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

# archive.ubuntu.com is served through a CDN that answers 502 on some networks. Set
# APT_MIRROR (for example http://mirror.yandex.ru/ubuntu) to build through another mirror.
ARG APT_MIRROR=

# yt-dlp is taken from the nightly channel on purpose: platform fixes (TikTok, YouTube) land
# there weeks before a stable release, and a stale yt-dlp means broken downloads.
# yt-dlp needs python3, FFmpeg is used to merge video and audio into MP4, and deno is the
# JavaScript runtime yt-dlp uses to read YouTube formats hidden behind JS.
# Both tools come from GitHub rather than from Docker Hub images, which are not reliably
# reachable from every network. The deno build is amd64, which is what we build and run on.
RUN if [ -n "$APT_MIRROR" ]; then \
        sed -i "s|http://archive.ubuntu.com/ubuntu|$APT_MIRROR|g; s|http://security.ubuntu.com/ubuntu|$APT_MIRROR|g" \
            /etc/apt/sources.list.d/ubuntu.sources; \
    fi \
    && apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg python3 curl ca-certificates \
    && curl -sSL https://github.com/yt-dlp/yt-dlp-nightly-builds/releases/latest/download/yt-dlp \
       -o /usr/local/bin/yt-dlp \
    && chmod +x /usr/local/bin/yt-dlp \
    && curl -sSL https://github.com/denoland/deno/releases/latest/download/deno-x86_64-unknown-linux-gnu.zip \
       -o /tmp/deno.zip \
    && python3 -m zipfile -e /tmp/deno.zip /usr/local/bin/ \
    && chmod +x /usr/local/bin/deno \
    && rm -f /tmp/deno.zip \
    && rm -rf /var/lib/apt/lists/*

# The base image already uses uid 1000, so let useradd pick a free one.
RUN useradd --create-home app

# A named volume inherits ownership from the image, so the directory must exist and belong
# to the application user. Without this the container cannot write into a fresh volume.
RUN mkdir -p /var/tmp/telegram-video && chown app:app /var/tmp/telegram-video
USER app

WORKDIR /app
COPY --from=build /build/target/telegram-video-*.jar app.jar

ENV VIDEO_DOWNLOAD_DIR=/tmp/telegram-video

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
