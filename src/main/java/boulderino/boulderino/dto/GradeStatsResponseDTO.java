package boulderino.boulderino.dto;

import boulderino.boulderino.entity.Grade;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GradeStatsResponseDTO {

    private Grade grade;

    private double successRate;

    private double flashRate;

    private double averageAttemptsUntilTop;

    private long amountOfTops;
    
    private long amountOfTriedBoulders;

    private long openProjects;

    private double averageTriesOnOpenProjects;
}