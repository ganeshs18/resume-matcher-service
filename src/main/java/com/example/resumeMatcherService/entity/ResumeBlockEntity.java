package com.example.resumeMatcherService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "resume_blocks")
public class ResumeBlockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private ResumeEntity resume;

    private Integer blockIndex;


    @Column(length = 2000)
    private String originalText;

    @Column(length = 2000)
    private String enhancedText;

    // Text formatting metadata
    private String font;
    private int fontSize;
    private boolean bold;
    private boolean italic;
    private boolean underline;

    private String alignment; // left, center, right, justify
    private float spacing;    // line spacing like 1.15

    private String blockType; // paragraph, header, table, hr, etc.
    private String styleClass; // optional for mapping to frontend styles

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime enhancedAt;


    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


}
