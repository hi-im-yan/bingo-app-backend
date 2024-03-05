package com.yanajiki.application.bingoapp.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

public class ConflictException extends RuntimeException {
    public ConflictException(String s) {
        super(s);
    }
}
