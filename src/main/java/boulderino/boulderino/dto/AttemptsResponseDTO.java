package boulderino.boulderino.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttemptsResponseDTO {

    private Long id;
    @PositiveOrZero
    private Integer tries;
    private Boolean done;
    private Boolean zone;
    private Long sessionId;
    private Long boulderId;
}