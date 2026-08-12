package com.finding.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoveGuideCreateDTO {
    @NotBlank @Size(max = 60) private String title;
    @NotBlank @Size(max = 100) private String subtitle;
    @NotBlank @Size(max = 5000) private String content;
    @NotBlank @Size(max = 30) private String category;
}
