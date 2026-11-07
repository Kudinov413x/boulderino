package boulderino.boulderino.dto;

import java.util.List;

import boulderino.boulderino.entity.Grade;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FormerWeaknessTopDTO {

    private BoulderInsightDTO boulder;
    private int tries;
    private List<BoulderAttributeInsightDTO> formerWeaknesses;
    private Grade grade;
}
