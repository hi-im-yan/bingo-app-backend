package com.yanajiki.application.bingoapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String s) {
        super(s);
    }
}
