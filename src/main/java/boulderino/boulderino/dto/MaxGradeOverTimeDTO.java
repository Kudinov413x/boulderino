package boulderino.boulderino.dto;

import boulderino.boulderino.entity.Grade;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MaxGradeOverTimeDTO {

    private LocalDate date;

    private Grade maxGrade;
}