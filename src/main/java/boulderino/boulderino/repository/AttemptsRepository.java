package boulderino.boulderino.repository;

import boulderino.boulderino.entity.Attempts;
import boulderino.boulderino.entity.Session;
import boulderino.boulderino.entity.Boulder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import boulderino.boulderino.entity.User;

import java.util.List;

public interface AttemptsRepository extends JpaRepository<Attempts, Long>{
    List<Attempts> findBySessionUser(User user);
    List<Attempts> findBySession(Session session);
    List<Attempts> findBySessionIdAndSessionUser(Long sessionId, User user);
    boolean existsByBoulderAndSessionUser(Boulder boulder, User user);
    List<Attempts> findByBoulder(Boulder boulder);
    List<Attempts> findByBoulderIn(List<Boulder> boulders);

    //TODO: Zählt Aktuell nicht Unique Zones, prüfen ob es das machen soll
    @Query("""
        SELECT
            a.boulder.id AS boulderId,
            COALESCE(SUM(a.tries), 0) AS totalTries,
            COALESCE(SUM(CASE WHEN a.done = true THEN 1 ELSE 0 END), 0) AS totalTops,
            COALESCE(SUM(CASE WHEN a.zone = true THEN 1 ELSE 0 END), 0) AS totalZones
        FROM Attempts a
        WHERE a.boulder IN :boulders
        GROUP BY a.boulder.id
    """)
    List<BoulderAttemptsStats> getStatsForBoulders(@Param("boulders") List<Boulder> boulders);
}

