package boulderino.boulderino.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatResponseDTO {

    private long totalSessions;

    private long sessionsThisWeek;

    private long sessionsThisMonth;

    private long totalBoulders;

    private long totalTops;

    private long totalZones;

    private double successRate;

    private long totalTries;
}