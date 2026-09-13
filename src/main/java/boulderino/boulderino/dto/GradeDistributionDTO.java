package boulderino.boulderino.dto;

import boulderino.boulderino.entity.Grade;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GradeDistributionDTO {

    private Grade grade;
    private double percentage;
    private long absoluteTops;
}