package boulderino.boulderino.dto;

import boulderino.boulderino.entity.Grade;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UntoppedGradeDistributionDTO {

    private Grade grade;
    private double percentage;
    private long absoluteCount;
}