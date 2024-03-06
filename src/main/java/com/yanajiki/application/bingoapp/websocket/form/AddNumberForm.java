package com.yanajiki.application.bingoapp.websocket.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class AddNumberForm {

    @JsonProperty("session-code")
    private String sessionCode;

    @JsonProperty("creator-hash")
    private String creatorHash;

    private Integer number;
}
