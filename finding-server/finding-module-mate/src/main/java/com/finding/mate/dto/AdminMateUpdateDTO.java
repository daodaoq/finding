package com.finding.mate.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AdminMateUpdateDTO {
    @NotBlank @Size(max = 100) private String title;
    @Size(max = 2000) private String description;
    @NotBlank
    @Pattern(regexp = "travel|carpool|fitness|study|exam|sports|gaming|entertainment|other")
    private String category;
    @NotBlank @Size(max = 200) private String location;
    @NotNull private LocalDateTime activityTime;
    @NotNull @Min(2) @Max(50) private Integer maxParticipants;
}
