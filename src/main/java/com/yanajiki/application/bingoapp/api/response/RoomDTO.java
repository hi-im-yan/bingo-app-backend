package com.yanajiki.application.bingoapp.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yanajiki.application.bingoapp.database.RoomEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoomDTO implements Serializable {

    private final String name;
    private final String description;
    private final String sessionCode;
    private final String creatorHash;
    private final String drawnNumbers;

    public static RoomDTO fromEntityToCreator(RoomEntity entity) {

        return new RoomDTO(
                entity.getName(),
                entity.getDescription(),
                entity.getSessionCode(),
                entity.getCreatorHash(),
                entity.getDrawnNumbers()
        );
    }

    public static RoomDTO fromEntityToPlayer(RoomEntity entity) {

        return new RoomDTO(
                entity.getName(),
                entity.getDescription(),
                entity.getSessionCode(),
                null,
                entity.getDrawnNumbers()
        );
    }
}
