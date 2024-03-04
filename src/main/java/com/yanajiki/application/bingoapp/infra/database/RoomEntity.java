package com.yanajiki.application.bingoapp.infra.database;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "room")
@NoArgsConstructor
@Getter
@Setter
public class RoomEntity {

    @Id
    private String id;

    private String name;
    private String password;
    private String description;
    private String sessionCode;
    private String creatorHash;
    private String drawnNumbers;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createDateTime;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updateDateTime;

}
