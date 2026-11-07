package boulderino.boulderino.dto;

import boulderino.boulderino.entity.GripType;
import boulderino.boulderino.entity.WallAngle;
import boulderino.boulderino.entity.ClimbingStyle;
import boulderino.boulderino.entity.RouteCharacter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoulderInsightDTO {

    private String name;
    private long id;

    private GripType gripType;
    private WallAngle wallAngle;
    private ClimbingStyle climbingStyle;
    private RouteCharacter routeCharacter;
}