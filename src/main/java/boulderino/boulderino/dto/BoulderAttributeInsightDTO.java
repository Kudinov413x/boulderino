package boulderino.boulderino.dto;

import boulderino.boulderino.entity.BoulderAttribute;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoulderAttributeInsightDTO {

    private BoulderAttribute attribute;
    private String value;
    private long matchingBoulders;
    private long totalBoulders;
    private double percentage;
}