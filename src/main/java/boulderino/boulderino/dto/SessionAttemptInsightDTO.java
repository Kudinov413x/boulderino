package boulderino.boulderino.dto;

import boulderino.boulderino.entity.Grade;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionAttemptInsightDTO {
    private BoulderInsightDTO boulder;
    private Grade grade;
    private int tries;
    private boolean done;
}