package boulderino.boulderino.dto;

import boulderino.boulderino.entity.Grade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoulderCreateDTO {

    @NotBlank
    private String name;

    @NotNull
    private Grade grade;
}