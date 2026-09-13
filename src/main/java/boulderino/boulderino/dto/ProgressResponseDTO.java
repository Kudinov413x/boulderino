package boulderino.boulderino.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProgressResponseDTO {

    private String maxGrade;

    private String avgGrade;

    private double flashRate;

    private long totalTriedBoulders;

    private long totalToppedBoulders;

    private double totalSuccessRate;
}