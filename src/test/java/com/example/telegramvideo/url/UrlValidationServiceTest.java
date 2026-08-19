package com.example.telegramvideo.url;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.telegramvideo.url.UrlValidationResult.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UrlValidationServiceTest {

    private final UrlValidationService service = new UrlValidationService();

    @ParameterizedTest
    @CsvSource({
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ, YOUTUBE",
            "https://youtube.com/watch?v=dQw4w9WgXcQ, YOUTUBE",
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ, YOUTUBE",
            "https://youtu.be/dQw4w9WgXcQ, YOUTUBE",
            "https://www.youtube.com/shorts/abc123, YOUTUBE",
            "https://www.tiktok.com/@user/video/123456, TIKTOK",
            "https://vm.tiktok.com/ZMabcdef/, TIKTOK",
            "https://www.instagram.com/reel/Abc123/, INSTAGRAM",
            "https://instagram.com/p/Abc123/, INSTAGRAM",
            "http://youtu.be/dQw4w9WgXcQ, YOUTUBE"
    })
    void detectsSupportedPlatforms(String url, Platform expected) {
        UrlValidationResult result = service.validate(url);

        assertThat(result.status()).isEqualTo(Status.VALID);
        assertThat(result.platform()).isEqualTo(expected);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void ignoresHostCaseAndSurroundingWhitespace() {
        UrlValidationResult result = service.validate("  https://WWW.YouTube.com/watch?v=dQw4w9WgXcQ  ");

        assertThat(result.platform()).isEqualTo(Platform.YOUTUBE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://vimeo.com/123456",
            "https://example.com/video.mp4",
            "https://notyoutube.com/watch?v=1",
            "https://youtube.com.evil.com/watch?v=1"
    })
    void rejectsUnsupportedPlatforms(String url) {
        UrlValidationResult result = service.validate(url);

        assertThat(result.status()).isEqualTo(Status.UNSUPPORTED_PLATFORM);
        assertThat(result.platform()).isNull();
        assertThat(result.isValid()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "just some text",
            "youtube.com/watch?v=1",
            "ftp://youtube.com/watch?v=1",
            "https://",
            "https://you tube.com/watch?v=1"
    })
    void rejectsInvalidUrls(String url) {
        UrlValidationResult result = service.validate(url);

        assertThat(result.status()).isEqualTo(Status.INVALID_URL);
        assertThat(result.platform()).isNull();
    }
}
