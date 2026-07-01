package com.finding.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MateVO {

    private Long id;
    private Long userId;
    private String category;
    private String categoryDesc;
    private String title;
    private String description;
    private LocalDateTime activityTime;
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private Integer isAnonymous;
    private Integer status;

    // Author info (masked if isAnonymous=1)
    private UserVO author;

    // Computed fields
    private Double distanceKm;
    private Boolean hasJoined;
    private Boolean isFull;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
