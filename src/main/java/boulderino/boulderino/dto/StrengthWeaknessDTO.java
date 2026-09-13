package boulderino.boulderino.dto;

import boulderino.boulderino.entity.BoulderAttribute;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StrengthWeaknessDTO {

    private BoulderAttribute attributeType;
    private String attribute;
    private double compositeScore;
}
