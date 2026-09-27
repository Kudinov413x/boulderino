package boulderino.boulderino.dto;

import java.time.LocalDate;

import boulderino.boulderino.entity.ClimbingStyle;
import boulderino.boulderino.entity.Grade;
import boulderino.boulderino.entity.GripType;
import boulderino.boulderino.entity.RouteCharacter;
import boulderino.boulderino.entity.WallAngle;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoulderResponseDTO {

    private Long id;

    private String name;

    private Grade grade;

    private WallAngle wallAngle;

    private GripType gripType;

    private RouteCharacter routeCharacter;

    private ClimbingStyle climbingStyle;

    private boolean firstAscent;
    private LocalDate firstAscentDate;
    private boolean flashed;

    private int totalTries;
    private int totalTops;
    private int totalZones;
}