package boulderino.boulderino.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SessionResponseDTO {

    private Long id;

    private LocalDate date;

    private String location;
}