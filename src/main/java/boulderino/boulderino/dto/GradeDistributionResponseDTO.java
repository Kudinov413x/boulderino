package boulderino.boulderino.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GradeDistributionResponseDTO {

    private long totalTops;

    private List<GradeDistributionDTO> grades;
}