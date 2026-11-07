package boulderino.boulderino.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardStatDTO {
    private long sessionsLastThirtyDays;
    private long topsLastThirtyDays;
    private long projectsLastThirtyDays;
    private long totalTriesLastThirtyDays;
}
