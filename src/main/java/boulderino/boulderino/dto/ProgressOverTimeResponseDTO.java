package boulderino.boulderino.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProgressOverTimeResponseDTO {

    private List<ProgressOverTimePointDTO> sessions;
}