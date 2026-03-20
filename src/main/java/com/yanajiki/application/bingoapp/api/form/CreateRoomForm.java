package com.yanajiki.application.bingoapp.api.form;

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
}
