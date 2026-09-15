package boulderino.boulderino.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FrontendTestDTO {

    private String username;
    private String maxGrade;
    private int totalTriedBoulders;
    private int totalToppedBoulders;

    public FrontendTestDTO(String u, String maxGrade, int totalTriedB, int totalTopB){
        this.username = u;
        this.maxGrade = maxGrade;
        this.totalTriedBoulders = totalTriedB;
        this.totalToppedBoulders = totalTopB;
    }
}
