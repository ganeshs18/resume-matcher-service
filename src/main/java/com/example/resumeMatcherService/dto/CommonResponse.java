package com.example.resumeMatcherService.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Setter
@Getter
public class CommonResponse<T> {
    private String message;
    private T data;
    private HttpStatus httpCode;

    public CommonResponse(String message, T data) {
        this.message = message;
        this.data = data;
        this.httpCode = HttpStatus.OK;
    }

    public CommonResponse(String message, T data, HttpStatus httpCode) {
        this.message = message;
        this.data = data;
        this.httpCode = httpCode;
    }

}
