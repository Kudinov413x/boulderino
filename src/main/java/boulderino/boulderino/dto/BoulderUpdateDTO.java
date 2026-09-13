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
public class BoulderUpdateDTO {

    private String name;

    private Grade grade;

    private WallAngle wallAngle;

    private GripType gripType;

    private RouteCharacter routeCharacter;

    private ClimbingStyle climbingStyle;
}