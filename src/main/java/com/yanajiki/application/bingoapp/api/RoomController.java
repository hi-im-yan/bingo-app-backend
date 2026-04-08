package com.yanajiki.application.bingoapp.api;

import com.yanajiki.application.bingoapp.api.form.CreateRoomForm;
import com.yanajiki.application.bingoapp.api.form.RoomLookupForm;
import com.yanajiki.application.bingoapp.api.response.ApiResponse;
import com.yanajiki.application.bingoapp.api.response.PlayerDTO;
import com.yanajiki.application.bingoapp.api.response.RoomDTO;
import com.yanajiki.application.bingoapp.service.QrCodeService;
import com.yanajiki.application.bingoapp.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for bingo room management.
 * <p>
 * Handles HTTP concerns only — request mapping, response wrapping, and exception handling.
 * All business logic is delegated to {@link RoomService}.
 * </p>
 */
@Tag(name = "Rooms", description = "Bingo room management")
@RestController
@RequestMapping("/api/v1/room")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

	private final RoomService roomService;
	private final QrCodeService qrCodeService;

	@Operation(
		summary = "Create a bingo room",
		description = "Creates a new bingo room and returns the creator view, including the creatorHash required for privileged operations."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "Room created successfully",
			content = @Content(schema = @Schema(implementation = RoomDTO.class))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "Validation error — name is blank or exceeds 255 characters",
			content = @Content(schema = @Schema(implementation = ApiResponse.class))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "409",
			description = "A room with the same name already exists",
			content = @Content(schema = @Schema(implementation = ApiResponse.class))
		)
	})
	@PostMapping
	public RoomDTO create(@Valid @RequestBody CreateRoomForm input) {
		return roomService.createRoom(input);
	}

	@Operation(
		summary = "Get a bingo room",
		description = "Returns the room identified by the given session code. "
			+ "If the X-Creator-Hash header is supplied and matches, the response includes the creatorHash (creator view). "
			+ "Otherwise the creatorHash is omitted (player view)."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "Room found",
			content = @Content(schema = @Schema(implementation = RoomDTO.class))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "404",
			description = "Room not found for the given session code",
			content = @Content(schema = @Schema(implementation = ApiResponse.class))
		)
	})
	@GetMapping("/{session-code}")
	public RoomDTO search(
		@Parameter(description = "Public session code of the room", required = true)
		@PathVariable("session-code") String sessionCode,
		@Parameter(description = "Optional creator hash; if valid, returns the creator view including creatorHash", required = false)
		@RequestHeader(value = "X-Creator-Hash", required = false) String creatorHash) {
		return roomService.findRoomBySessionCode(sessionCode, creatorHash);
	}

	@Operation(
		summary = "Delete a bingo room",
		description = "Permanently deletes the room identified by the given session code. "
			+ "Requires the X-Creator-Hash header to authenticate the request as the room creator."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "Room deleted successfully"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "Invalid or missing creator hash",
			content = @Content(schema = @Schema(implementation = ApiResponse.class))
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "404",
			description = "Room not found for the given session code",
			content = @Content(schema = @Schema(implementation = ApiResponse.class))
		)
	})
	@DeleteMapping("/{session-code}")
	public void delete(
		@Parameter(description = "Public session code of the room", required = true)
		@PathVariable("session-code") String sessionCode,
		@Parameter(description = "Creator hash for authentication; must match the hash assigned at room creation", required = true)
		@RequestHeader("X-Creator-Hash") String creatorHash) {
		roomService.deleteRoom(sessionCode, creatorHash);
	}

	@Operation(
		summary = "Get QR code for a bingo room",
		description = "Returns a 250x250 PNG QR code that encodes the join URL for the given room. "
			+ "No authentication required. Returns 404 if the room does not exist."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "QR code PNG image",
			content = @Content(mediaType = "image/png")
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "404",
			description = "Room not found for the given session code",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))
		)
	})
	@GetMapping(value = "/{session-code}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> getQrCode(
		@Parameter(description = "Public session code of the room", required = true)
		@PathVariable("session-code") String sessionCode) {
		roomService.findRoomBySessionCode(sessionCode, null);
		byte[] imageBytes = qrCodeService.generateQrCodeForRoom(sessionCode);
		return ResponseEntity.ok()
			.contentType(MediaType.IMAGE_PNG)
			.body(imageBytes);
	}

	@Operation(
		summary = "Lookup rooms by creator hashes",
		description = "Returns the rooms matching the supplied creator hashes in creator view (including creatorHash). "
			+ "Unknown hashes are silently skipped. A null or empty list yields an empty result. Never returns 404."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "List of matching rooms (possibly empty)",
			content = @Content(schema = @Schema(implementation = RoomDTO.class))
		)
	})
	@PostMapping("/lookup")
	public List<RoomDTO> lookup(@RequestBody RoomLookupForm form) {
		return roomService.findRoomsByCreatorHashes(
			form.creatorHashes() == null ? List.of() : form.creatorHashes()
		);
	}

	@Operation(
		summary = "List players in a room",
		description = "Returns all players who have joined the room. Requires creator authentication via X-Creator-Hash header."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "Player list retrieved successfully"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "404",
			description = "Room not found or invalid creator hash",
			content = @Content(schema = @Schema(implementation = ApiResponse.class))
		)
	})
	@GetMapping("/{session-code}/players")
	public List<PlayerDTO> getPlayers(
		@Parameter(description = "Public session code of the room", required = true)
		@PathVariable("session-code") String sessionCode,
		@Parameter(description = "Creator hash for authentication; must match the hash assigned at room creation", required = true)
		@RequestHeader("X-Creator-Hash") String creatorHash) {
		return roomService.getPlayersByRoom(sessionCode, creatorHash);
	}

}
