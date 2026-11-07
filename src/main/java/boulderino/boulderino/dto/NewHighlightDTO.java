package boulderino.boulderino.dto;

import boulderino.boulderino.entity.HighlightType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewHighlightDTO {

    private HighlightType type;
    private String message;

    // Optional: Der Boulder, bei dem das Highlight entstanden ist
    private SessionAttemptInsightDTO sessionAttemptInsightDTO;
}
