package boulderino.boulderino.repository;

import boulderino.boulderino.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import boulderino.boulderino.entity.User;

import java.time.LocalDate;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long>{
    List<Session> findByUser(User user);

    long countByUser(User user);
    long countByUserAndDateBetween( User user, LocalDate startDate, LocalDate endDate );
}
