package com.yanajiki.application.bingoapp.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ApiResponse {

    private final int status;
    private final String message;
}
