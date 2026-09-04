package boulderino.boulderino.dto;

import boulderino.boulderino.entity.Grade;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoulderResponseDTO {
    private String name;
    private Grade grade;
    private Long id;
}