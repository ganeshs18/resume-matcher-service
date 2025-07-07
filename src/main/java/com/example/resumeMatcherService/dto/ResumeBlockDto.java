package com.example.resumeMatcherService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeBlockDto {
    private Long id;
    private Integer blockIndex;

    private String originalText;
    private String enhancedText; // Optional - may be null if not enhanced yet

    // Metadata
    private String font;
    private int fontSize;
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private String alignment; // e.g., "left", "center", "right", "justify"
    private float spacing;

    // Optional extras (if needed later)
    private String blockType; // e.g., "paragraph", "table", "header", "hr", etc.
    private String styleClass; // for mapping into CSS or Angular editor styles

    public static ResumeBlockDto fromEntity(com.example.resumeMatcherService.entity.ResumeBlockEntity entity) {
        if (entity == null) return null;
        ResumeBlockDto dto = new ResumeBlockDto();
        dto.setId(entity.getId());
        dto.setBlockIndex(entity.getBlockIndex());
        dto.setOriginalText(entity.getOriginalText());
        dto.setEnhancedText(entity.getEnhancedText());
        dto.setFont(entity.getFont());
        dto.setFontSize(entity.getFontSize());
        dto.setBold(entity.isBold());
        dto.setItalic(entity.isItalic());
        dto.setUnderline(entity.isUnderline());
        dto.setAlignment(entity.getAlignment());
        dto.setSpacing(entity.getSpacing());
        dto.setBlockType(entity.getBlockType());
        // styleClass is not present in entity, so leave as null
        return dto;
    }
}
