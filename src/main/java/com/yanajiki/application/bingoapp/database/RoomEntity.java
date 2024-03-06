package com.yanajiki.application.bingoapp.database;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "room")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RoomEntity {

    private static final int SESSION_CODE_LENGTH = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true)
    private String name;

//    private String password;

    @Column(name = "description")
    private String description;

    @Column(name = "session_code", unique = true)
    private String sessionCode;

    @Column(name = "creator_hash", unique = true)
    private String creatorHash;

    @Column(name = "drawn_numbers")
    private String drawnNumbers = "";

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createDateTime;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updateDateTime;

    public static RoomEntity createEntityObject(String name, String description) {
        String creatorHash = UUID.randomUUID().toString();

        RoomEntity roomEntity = new RoomEntity();
        roomEntity.setName(name);
        roomEntity.setDescription(description);
        roomEntity.setCreatorHash(creatorHash);
        roomEntity.setCreateDateTime(LocalDateTime.now());
        roomEntity.setUpdateDateTime(LocalDateTime.now());
        roomEntity.setDrawnNumbers("");
        roomEntity.setSessionCode(newSessionCode());
        return roomEntity;
    }

    public void setDrawnNumbers(String drawnNumber) {
        ArrayList<String> list = new ArrayList<>(Arrays.asList(this.drawnNumbers.split(",")));
        list.add(drawnNumber);
        this.drawnNumbers = String.join(",", list);
    }

    private static String newSessionCode() {
        String allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        StringBuilder randomChars = new StringBuilder();

        Random random = new Random();

        for (int i = 0; i < SESSION_CODE_LENGTH; i++) {
            int randomIndex = random.nextInt(allowedChars.length());
            char randomChar = allowedChars.charAt(randomIndex);
            randomChars.append(randomChar);
        }

        return randomChars.toString();
    }
}
