package boulderino.boulderino.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SessionCreateDTO {
    
    @NotBlank
    private String location;
}