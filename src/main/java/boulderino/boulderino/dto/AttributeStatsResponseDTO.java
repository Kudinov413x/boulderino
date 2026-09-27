package boulderino.boulderino.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttributeStatsResponseDTO {
    private String attribute;
    private long amountOfTriedBoulders;
    private long amountOfTops;
    private long openProjects;
    private long amountOfFlashes;
    private long amountOfZones;
    private double successRate;
    private double flashRate;
    private double averageAttemptsUntilTop;
    private double averageTriesOnOpenProjects;
    private String avgGrade;
    private String maxGrade;
    private String highestProjectZoneGrade;
    private String avgGradeOnOpenProjects;
}