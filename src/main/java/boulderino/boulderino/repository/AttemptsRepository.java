package boulderino.boulderino.repository;

import boulderino.boulderino.entity.Attempts;
import boulderino.boulderino.entity.Session;
import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDate;

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

    // Für DashboardStatDTO
    @Query("""
    SELECT COUNT(DISTINCT a.boulder.id)
    FROM Attempts a
    WHERE a.session.user = :user
    AND a.session.date BETWEEN :startDate AND :endDate
    AND a.done = true
    """)
    long countUniqueTopsLastThirtyDays(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query("""
        SELECT COUNT(DISTINCT a.boulder.id)
        FROM Attempts a
        WHERE a.session.user = :user
        AND a.session.date BETWEEN :startDate AND :endDate
        AND a.boulder.firstAscent = false
    """)
    long countUniqueProjectsLastThirtyDays(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    @Query("""
        SELECT COALESCE(SUM(a.tries), 0)
        FROM Attempts a
        WHERE a.session.user = :user
        AND a.session.date BETWEEN :startDate AND :endDate
    """)
    long sumTriesLastThirtyDays(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT DISTINCT a.session
        FROM Attempts a
        WHERE a.boulder = :boulder
        AND a.session.user = :user
        ORDER BY a.session.date DESC, a.session.id DESC
    """)
    List<Session> findSessionsByBoulderAndUser(
            @Param("boulder") Boulder boulder,
            @Param("user") User user
    );
}

