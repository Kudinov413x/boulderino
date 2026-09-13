package boulderino.boulderino.dto;

import boulderino.boulderino.entity.ClimbingStyle;
import boulderino.boulderino.entity.Grade;
import boulderino.boulderino.entity.GripType;
import boulderino.boulderino.entity.RouteCharacter;
import boulderino.boulderino.entity.WallAngle;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionDetailsResponseDTO {

    private Long boulderId;

    private String boulderName;

    private Grade grade;

    private GripType gripType;

    private WallAngle wallAngle;

    private RouteCharacter routeCharacter;

    private ClimbingStyle climbingStyle;

    private Long attemptId;

    private int tries;

    private boolean zone;

    private boolean done;
}