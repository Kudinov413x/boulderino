package boulderino.boulderino.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionStatsDTO {
    private long sessionTries;
    private long sessionBoulders;
    private long sessionTops;
    private long sessionZones;
    private long sessionFlashes;
    private double successRate;
    private double flashRate;
    private List<String> labels;
}
