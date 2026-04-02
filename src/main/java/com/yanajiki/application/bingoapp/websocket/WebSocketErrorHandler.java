package com.yanajiki.application.bingoapp.websocket;

import com.yanajiki.application.bingoapp.api.response.ErrorResponse;
import com.yanajiki.application.bingoapp.exception.BingoException;
import com.yanajiki.application.bingoapp.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Catches exceptions thrown by {@code @MessageMapping} handlers and sends structured
 * JSON error responses to the caller's personal error queue.
 * <p>
 * Clients must subscribe to {@code /user/queue/errors} to receive these responses.
 * </p>
 */
@ControllerAdvice
@Slf4j
public class WebSocketErrorHandler {

	/**
	 * Handles {@link BingoException} subtypes (BadRequestException, ConflictException,
	 * RoomNotFoundException) thrown during WebSocket message handling.
	 *
	 * @param ex the bingo domain exception
	 * @return structured error response sent to the caller's error queue
	 */
	@MessageExceptionHandler(BingoException.class)
	@SendToUser("/queue/errors")
	public ErrorResponse handleBingoException(BingoException ex) {
		log.warn("WebSocket error [{}]: {}", ex.getErrorCode(), ex.getMessage());
		HttpStatus status = resolveStatus(ex);
		return new ErrorResponse(status.value(), ex.getErrorCode().name(), ex.getMessage());
	}

	/**
	 * Catches any unexpected exception during WebSocket message handling.
	 *
	 * @param ex the unexpected exception
	 * @return a generic error response sent to the caller's error queue
	 */
	@MessageExceptionHandler(Exception.class)
	@SendToUser("/queue/errors")
	public ErrorResponse handleUnknown(Exception ex) {
		log.error("Unexpected WebSocket error", ex);
		return new ErrorResponse(500, ErrorCode.INTERNAL_ERROR.name(), "If the error persists, open a ticket.");
	}

	/**
	 * Resolves the HTTP status from the {@link ResponseStatus} annotation on the exception class.
	 *
	 * @param ex the bingo exception
	 * @return the resolved HTTP status, or 500 if no annotation is present
	 */
	private HttpStatus resolveStatus(BingoException ex) {
		ResponseStatus annotation = ex.getClass().getAnnotation(ResponseStatus.class);
		return annotation != null ? annotation.value() : HttpStatus.INTERNAL_SERVER_ERROR;
	}
}
