package com.yanajiki.application.bingoapp.api.form;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class CreateRoomForm {

    private String name;
    private String description;
    private String password;
}
