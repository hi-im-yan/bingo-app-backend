package com.yanajiki.application.bingoapp.service;

import com.yanajiki.application.bingoapp.api.response.NumberCorrectionDTO;
import com.yanajiki.application.bingoapp.api.response.RoomDTO;

/**
 * Holds the result of a number correction: the updated room state and the correction notification.
 *
 * @param roomDTO       the updated room in player view (for state broadcast)
 * @param correctionDTO the correction details (for notification broadcast)
 */
public record CorrectionResult(RoomDTO roomDTO, NumberCorrectionDTO correctionDTO) {
}
