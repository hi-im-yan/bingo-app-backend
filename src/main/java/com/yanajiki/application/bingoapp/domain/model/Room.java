package com.yanajiki.application.bingoapp.domain.model;

import com.yanajiki.application.bingoapp.domain.vo.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Room {

    private Id id;
    private Name name;
    private Password password;
    private String description;
    private SessionCode sessionCode;

    // this will be the creatorHash that will grant permission that only the creator can have
    // is a weak way to protect but i dont want to complicate things....
    private CreatorHash creatorHash;
    private String drawnNumbers;
}
