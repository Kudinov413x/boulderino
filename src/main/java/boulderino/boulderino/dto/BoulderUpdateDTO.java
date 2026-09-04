package boulderino.boulderino.dto;

import boulderino.boulderino.entity.Grade;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoulderUpdateDTO {

    private String name;
    private Grade grade;
}