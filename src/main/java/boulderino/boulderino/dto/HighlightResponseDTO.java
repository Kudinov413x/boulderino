package boulderino.boulderino.dto;

import boulderino.boulderino.entity.HighlightType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HighlightResponseDTO {

    private HighlightType type;
    private String title;
    private String description;
    private LocalDate date;
    private String grade;
    private Integer tries;
    private Double significance;
    private Long sessionId;
    private Long attemptId;
}