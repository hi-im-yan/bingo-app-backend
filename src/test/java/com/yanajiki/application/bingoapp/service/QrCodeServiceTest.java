package com.yanajiki.application.bingoapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link QrCodeService}.
 * <p>
 * Verifies URL construction and QR code PNG generation without spinning up
 * the full Spring context. The {@code joinBaseUrl} field is injected directly
 * via {@link ReflectionTestUtils} to avoid Spring's {@code @Value} wiring.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class QrCodeServiceTest {

	private QrCodeService qrCodeService;

	@BeforeEach
	void setUp() {
		qrCodeService = new QrCodeService();
		ReflectionTestUtils.setField(qrCodeService, "joinBaseUrl", "http://localhost:8080/join");
	}

	// ─── buildJoinUrl ──────────────────────────────────────────────────────────

	@Nested
	@DisplayName("buildJoinUrl")
	class BuildJoinUrl {

		/**
		 * The join URL must be a concatenation of the base URL and the session code
		 * separated by a forward slash.
		 */
		@Test
		@DisplayName("returns base URL concatenated with session code")
		void returnsBaseUrlConcatenatedWithSessionCode() {
			// when
			String url = qrCodeService.buildJoinUrl("ABC123");

			// then
			assertThat(url).isEqualTo("http://localhost:8080/join/ABC123");
		}

		/**
		 * Different session codes must produce distinct join URLs, confirming the
		 * session code is appended dynamically each time.
		 */
		@Test
		@DisplayName("different session codes produce different URLs")
		void differentSessionCodesProduceDifferentUrls() {
			// when
			String url1 = qrCodeService.buildJoinUrl("AAAAAA");
			String url2 = qrCodeService.buildJoinUrl("BBBBBB");

			// then
			assertThat(url1).isNotEqualTo(url2);
			assertThat(url1).endsWith("/AAAAAA");
			assertThat(url2).endsWith("/BBBBBB");
		}
	}

	// ─── generateQrCodeForRoom ────────────────────────────────────────────────

	@Nested
	@DisplayName("generateQrCodeForRoom")
	class GenerateQrCodeForRoom {

		/**
		 * The method must return a non-empty byte array.
		 * Any valid PNG QR code will always have bytes.
		 */
		@Test
		@DisplayName("returns non-empty byte array")
		void returnsNonEmptyByteArray() {
			// when
			byte[] result = qrCodeService.generateQrCodeForRoom("ABC123");

			// then
			assertThat(result).isNotEmpty();
		}

		/**
		 * The returned bytes must begin with the PNG magic number {@code 0x89504E47}
		 * (the four-byte signature that every valid PNG file starts with).
		 */
		@Test
		@DisplayName("returned bytes start with PNG magic number (0x89504E47)")
		void returnedBytesStartWithPngMagicNumber() {
			// when
			byte[] result = qrCodeService.generateQrCodeForRoom("ABC123");

			// then — PNG magic bytes: 0x89 0x50 0x4E 0x47
			assertThat(result).hasSizeGreaterThan(4);
			assertThat(result[0]).isEqualTo((byte) 0x89);
			assertThat(result[1]).isEqualTo((byte) 0x50);
			assertThat(result[2]).isEqualTo((byte) 0x4E);
			assertThat(result[3]).isEqualTo((byte) 0x47);
		}
	}
}
