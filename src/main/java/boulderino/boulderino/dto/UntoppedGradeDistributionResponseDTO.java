package boulderino.boulderino.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UntoppedGradeDistributionResponseDTO {

    private long totalUntopped;
    private long zoneCount;
    private double zoneRate;

    private List<UntoppedGradeDistributionDTO> grades;
}