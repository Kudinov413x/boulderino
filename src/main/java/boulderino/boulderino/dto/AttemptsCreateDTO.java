package boulderino.boulderino.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttemptsCreateDTO {

    @PositiveOrZero
    private int tries;
    private boolean done;
    private boolean zone;
    @NotNull
    private Long sessionId;
    @NotNull
    private Long boulderId;
}