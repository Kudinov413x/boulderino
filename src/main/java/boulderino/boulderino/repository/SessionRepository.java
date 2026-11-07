package boulderino.boulderino.repository;

import boulderino.boulderino.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import boulderino.boulderino.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long>{
    List<Session> findByUserOrderByDateDescIdDesc(User user);
    List<Session> findByUser(User user);
    List<Session> findByUserAndDate(User user, LocalDate date);

    long countByUser(User user);
    long countByUserAndDateBetween( User user, LocalDate startDate, LocalDate endDate);
    Optional<Session> findFirstByUserAndDateBeforeOrderByDateDescIdDesc(User user, LocalDate date);
    Optional<Session> findByIdAndUser(Long sessionId, User user);
}
