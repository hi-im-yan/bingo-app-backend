package com.yanajiki.application.bingoapp.api.form;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for partial updates of a bingo room.
 * <p>
 * Mutable class (not a record) so Jackson can deserialize partial JSON bodies.
 * PATCH semantic: a {@code null} field means "no change"; an empty string clears
 * the target field.
 * </p>
 */
@NoArgsConstructor
@Getter
@Setter
public class UpdateRoomForm {

	/** New description for the room. {@code null} = no change; {@code ""} = clear; otherwise update. At most 255 characters. */
	@Size(max = 255)
	private String description;
}
