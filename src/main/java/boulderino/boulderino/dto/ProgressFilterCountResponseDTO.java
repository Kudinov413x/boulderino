package boulderino.boulderino.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProgressFilterCountResponseDTO {

    private List<ProgressFilterCountDTO> gripTypes;
    private List<ProgressFilterCountDTO> wallAngles;
    private List<ProgressFilterCountDTO> climbingStyles;
    private List<ProgressFilterCountDTO> routeCharacters;
    private List<ProgressFilterCountDTO> minGrades;
    private List<ProgressFilterCountDTO> maxGrades;
}