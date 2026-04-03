package com.yanajiki.application.bingoapp.websocket;

import com.yanajiki.application.bingoapp.api.response.ErrorResponse;
import com.yanajiki.application.bingoapp.exception.BadRequestException;
import com.yanajiki.application.bingoapp.exception.BingoException;
import com.yanajiki.application.bingoapp.exception.ConflictException;
import com.yanajiki.application.bingoapp.exception.ErrorCode;
import com.yanajiki.application.bingoapp.exception.RoomNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

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

	@MessageExceptionHandler(BingoException.class)
	@SendToUser("/queue/errors")
	public ErrorResponse handleBingoException(BingoException ex) {
		log.warn("WebSocket error [{}]: {}", ex.getErrorCode(), ex.getMessage());
		HttpStatus status = resolveStatus(ex);
		return new ErrorResponse(status.value(), ex.getErrorCode().name(), ex.getMessage());
	}

	@MessageExceptionHandler(Exception.class)
	@SendToUser("/queue/errors")
	public ErrorResponse handleUnknown(Exception ex) {
		log.error("Unexpected WebSocket error", ex);
		return new ErrorResponse(500, ErrorCode.INTERNAL_ERROR.name(), "If the error persists, open a ticket.");
	}

	private HttpStatus resolveStatus(BingoException ex) {
		if (ex instanceof RoomNotFoundException) return HttpStatus.NOT_FOUND;
		if (ex instanceof ConflictException) return HttpStatus.CONFLICT;
		if (ex instanceof BadRequestException) return HttpStatus.BAD_REQUEST;
		return HttpStatus.INTERNAL_SERVER_ERROR;
	}
}
