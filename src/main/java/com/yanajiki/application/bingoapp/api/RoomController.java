package com.yanajiki.application.bingoapp.api;

import com.yanajiki.application.bingoapp.api.form.CreateRoomForm;
import com.yanajiki.application.bingoapp.api.response.ApiResponse;
import com.yanajiki.application.bingoapp.api.response.RoomDTO;
import com.yanajiki.application.bingoapp.database.RoomEntity;
import com.yanajiki.application.bingoapp.database.RoomRepository;
import com.yanajiki.application.bingoapp.exception.ConflictException;
import com.yanajiki.application.bingoapp.exception.RoomNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

@RestController
@RequestMapping("/api/v1/room")
@AllArgsConstructor
@Slf4j
public class RoomController {

    private final RoomRepository repository;

    @PostMapping
    public RoomDTO create(@RequestBody CreateRoomForm input) {
        System.out.println("Creating room..");
        repository.findByName(input.getName())
                .ifPresent(entity -> {
                    throw new ConflictException("Room already exists.");
                });

        RoomEntity entityObject = RoomEntity.createEntityObject(input.getName(), input.getDescription());
        RoomEntity save = repository.save(entityObject);

        return RoomDTO.fromEntityToCreator(save);
    }

    @GetMapping("/{session-code}")
    public RoomDTO search(@PathVariable("session-code") String sessionCode,
                          @RequestParam(value = "creator-hash", required = false) String creatorHash) {

        if (StringUtils.isNotBlank(creatorHash)) {
            RoomEntity roomEntity = repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash)
                    .orElseThrow(() -> new RoomNotFoundException("not found"));
            return RoomDTO.fromEntityToCreator(roomEntity);
        }

        RoomEntity roomEntity = repository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new RoomNotFoundException("not found"));

        return RoomDTO.fromEntityToPlayer(roomEntity);
    }

    @DeleteMapping("/{session-code}/{creator-hash}")
    public void delete(@PathVariable("session-code") String sessionCode,
                       @PathVariable("creator-hash") String creatorHash) {
        RoomEntity roomEntity = repository.findBySessionCodeAndCreatorHash(sessionCode, creatorHash)
                .orElseThrow(() -> new RoomNotFoundException("not found"));

        repository.delete(roomEntity);
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse> handleConflct(ConflictException ex) {
        int httpStatus = HttpStatus.CONFLICT.value();
        ApiResponse apiResponse = new ApiResponse(httpStatus, ex.getMessage());
        return ResponseEntity.status(httpStatus).body(apiResponse);
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse> handleNotFound(RoomNotFoundException ex) {
        int httpStatus = HttpStatus.NOT_FOUND.value();
        ApiResponse apiResponse = new ApiResponse(httpStatus, ex.getMessage());
        return ResponseEntity.status(httpStatus).body(apiResponse);
    }

    @ExceptionHandler
    public ResponseEntity<ApiResponse> handleUnknown(Exception ex, WebRequest request) {
        log.error("UNKNOWN_ERROR:: {}", ex);
        int httpStatus = HttpStatus.INTERNAL_SERVER_ERROR.value();
        ApiResponse apiResponse = new ApiResponse(httpStatus, "If the error persists. Open a ticket.");
        return ResponseEntity.status(httpStatus).body(apiResponse);
    }

}
