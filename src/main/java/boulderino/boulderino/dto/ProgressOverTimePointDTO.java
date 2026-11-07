package boulderino.boulderino.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProgressOverTimePointDTO {

    private LocalDate date;

    private String maxGrade;
    private Integer maxGradeValue;

    private String maxFlashGrade;
    private Integer maxFlashGradeValue;

    private String maxProjectZoneGrade;
    private Integer maxProjectZoneGradeValue;
}