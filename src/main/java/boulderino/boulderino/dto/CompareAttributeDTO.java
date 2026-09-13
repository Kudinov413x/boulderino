package boulderino.boulderino.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompareAttributeDTO {

    private String attribute;

    private long amountOfTriedBoulders;

    private long amountOfTops;

    private long openProjects;

    private double successRate;

    private double flashRate;

    private double averageAttemptsUntilTop;

    private double averageTriesOnOpenProjects;

    private double gradeAvgTop;

    private double gradeAvgFlash;

    
}
