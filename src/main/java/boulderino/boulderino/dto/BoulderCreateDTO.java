package boulderino.boulderino.dto;

import boulderino.boulderino.entity.Grade;
import boulderino.boulderino.entity.GripType;
import boulderino.boulderino.entity.WallAngle;
import boulderino.boulderino.entity.RouteCharacter;
import boulderino.boulderino.entity.ClimbingStyle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoulderCreateDTO {

    @NotBlank
    private String name;

    @NotNull
    private Grade grade;

    @NotNull
    private WallAngle wallAngle;

    @NotNull
    private GripType gripType;

    @NotNull
    private RouteCharacter routeCharacter;

    @NotNull
    private ClimbingStyle climbingStyle;
}