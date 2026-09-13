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

    private double successRate;

    private double flashRate;

    private double averageAttemptsUntilTop;

    private double averageTriesOnOpenProjects;
}