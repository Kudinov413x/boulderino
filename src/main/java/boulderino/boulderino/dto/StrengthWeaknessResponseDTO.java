package boulderino.boulderino.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StrengthWeaknessResponseDTO {

    private List<StrengthWeaknessDTO> strengths;
    private List<StrengthWeaknessDTO> weaknesses;

}
