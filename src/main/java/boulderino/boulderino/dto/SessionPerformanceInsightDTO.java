package boulderino.boulderino.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SessionPerformanceInsightDTO {
    private String metric;
    private double currentValue;
    private double medianValue;
    private double improvement;
}