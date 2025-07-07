package com.example.resumeMatcherService.dto;

public class CommonErrorResponse {
    private boolean success;

    public CommonErrorResponse() {
        this.success = false;
    }

    public CommonErrorResponse(String message, String errorCode) {
        this.success = false;
    }


    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}


