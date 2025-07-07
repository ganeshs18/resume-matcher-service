package com.example.resumeMatcherService.dto;

public class CommonErrorException extends RuntimeException {
    private final CommonErrorResponse errorResponse;

    public CommonErrorException(String message, String errorCode) {
        super(message);
        this.errorResponse = new CommonErrorResponse(message, errorCode);
    }

    public CommonErrorResponse getErrorResponse() {
        return errorResponse;
    }
}

