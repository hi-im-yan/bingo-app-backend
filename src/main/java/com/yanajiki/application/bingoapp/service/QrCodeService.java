package com.yanajiki.application.bingoapp.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.yanajiki.application.bingoapp.exception.BadRequestException;
import com.yanajiki.application.bingoapp.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service responsible for generating QR code images for bingo rooms.
 * <p>
 * Builds a join URL from the configured base URL and a room's session code,
 * then encodes that URL as a PNG QR code using the ZXing library.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeService {

	/** Size (width and height) of the generated QR code image in pixels. */
	private static final int QR_CODE_SIZE = 250;

	/** Image format written to the output stream. */
	private static final String IMAGE_FORMAT = "PNG";

	@Value("${app.join-base-url}")
	private String joinBaseUrl;

	/**
	 * Generates a QR code PNG image encoding the join URL for the given room.
	 *
	 * @param sessionCode the public session code of the room
	 * @return a byte array containing the PNG image data
	 * @throws IllegalStateException if QR code generation fails due to an internal ZXing or I/O error
	 */
	public byte[] generateQrCodeForRoom(String sessionCode) {
		String joinUrl = buildJoinUrl(sessionCode);
		log.debug("Generating QR code for URL '{}'", joinUrl);

		try {
			QRCodeWriter writer = new QRCodeWriter();
			BitMatrix bitMatrix = writer.encode(joinUrl, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			MatrixToImageWriter.writeToStream(bitMatrix, IMAGE_FORMAT, outputStream);

			byte[] imageBytes = outputStream.toByteArray();
			log.debug("QR code generated successfully for session code '{}', size={} bytes", sessionCode, imageBytes.length);
			return imageBytes;

		} catch (WriterException | IOException e) {
			log.error("Failed to generate QR code for session code '{}'", sessionCode, e);
			throw new BadRequestException(ErrorCode.INTERNAL_ERROR, "Failed to generate QR code for session code: " + sessionCode);
		}
	}

	/**
	 * Builds the full join URL for a given room session code.
	 * <p>
	 * Package-private to allow direct testing without Spring context wiring.
	 * </p>
	 *
	 * @param sessionCode the public session code of the room
	 * @return the full join URL as a string, e.g. {@code http://localhost:8080/join/ABC123}
	 */
	String buildJoinUrl(String sessionCode) {
		return joinBaseUrl + "/" + sessionCode;
	}
}
