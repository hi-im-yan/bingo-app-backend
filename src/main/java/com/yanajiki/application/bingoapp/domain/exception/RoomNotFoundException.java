package com.yanajiki.application.bingoapp.domain.exception;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String s) {
        super(s);
    }
}
