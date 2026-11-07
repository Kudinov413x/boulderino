package boulderino.boulderino.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ProgressFilterCountDTO {
    private String value;
    private long count;
}
