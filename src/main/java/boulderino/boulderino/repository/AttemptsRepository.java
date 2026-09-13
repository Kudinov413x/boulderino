package boulderino.boulderino.repository;

import boulderino.boulderino.entity.Attempts;
import boulderino.boulderino.entity.Session;
import boulderino.boulderino.entity.Boulder;
import org.springframework.data.jpa.repository.JpaRepository;
import boulderino.boulderino.entity.User;

import java.util.List;

public interface AttemptsRepository extends JpaRepository<Attempts, Long>{
    List<Attempts> findBySessionUser(User user);
    List<Attempts> findBySession(Session session);
    List<Attempts> findBySessionIdAndSessionUser(Long sessionId, User user);
    boolean existsByBoulderAndSessionUser(Boulder boulder, User user);
    List<Attempts> findByBoulder(Boulder boulder);
    List<Attempts> findByBoulderIn(List<Boulder> boulders);
}
