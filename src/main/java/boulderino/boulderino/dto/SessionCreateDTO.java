package boulderino.boulderino.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SessionCreateDTO {
    private String name;

    @NotBlank
    private String location;
}