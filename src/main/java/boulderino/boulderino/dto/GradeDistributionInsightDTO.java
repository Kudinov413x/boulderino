package boulderino.boulderino.dto;

import boulderino.boulderino.entity.Grade;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GradeDistributionInsightDTO {

    private String type;
    private Grade grade;
    private long value;
    private double percentage;
}