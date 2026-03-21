package com.yanajiki.application.bingoapp.api.form;

import com.yanajiki.application.bingoapp.game.DrawMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for creating a new bingo room.
 * <p>
 * Kept as a mutable class (not a record) so that Jackson can deserialize it
 * without requiring a fully-qualified constructor call.
 * </p>
 * <p>
 * {@code drawMode} is optional; when omitted (null), the service defaults to {@link DrawMode#MANUAL}.
 * </p>
 */
@NoArgsConstructor
@Getter
@Setter
public class CreateRoomForm {

	/** The unique display name for the room. Must not be blank and at most 255 characters. */
	@NotBlank
	@Size(max = 255)
	private String name;

	/** An optional description for the room. At most 255 characters. */
	@Size(max = 255)
	private String description;

	/**
	 * The draw mode for the room. Optional — if not provided, the service defaults to
	 * {@link DrawMode#MANUAL}. Use {@link DrawMode#AUTOMATIC} to enable server-side
	 * random number drawing.
	 */
	private DrawMode drawMode;
}
