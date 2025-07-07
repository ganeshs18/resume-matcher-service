package com.example.resumeMatcherService.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumeEvent {

    public ResumeEvent(EventType eventType, String message, Object block, int progress) {
        this.type = eventType;
        this.message = message;
        this.block = (block instanceof ResumeBlockDto) ? (ResumeBlockDto) block : null;
        this.progress = progress;
    }

    public enum EventType {
        UPLOAD_START,
        UPLOAD_SUCCESS,
        PARSING_BLOCKS,
        BLOCK_ENHANCED,
        COMPLETE,
        AI_RESPONSE_RECEIVED, ERROR,
        AI_RAW_STREAM
    }

    private EventType type;
    private String message;
    private ResumeBlockDto block; // Optional, only for BLOCK_ENHANCED
    private int progress;

    // Constructors, Getters, Setters
}
