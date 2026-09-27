package boulderino.boulderino.repository;

public interface BoulderAttemptsStats {

    Long getBoulderId();

    Integer getTotalTries();

    Long getTotalTops();

    Long getTotalZones();
}

