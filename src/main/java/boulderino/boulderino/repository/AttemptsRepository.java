package boulderino.boulderino.repository;

import boulderino.boulderino.entity.Attempts;
import org.springframework.data.jpa.repository.JpaRepository;
import boulderino.boulderino.entity.User;

import java.util.List;

public interface AttemptsRepository extends JpaRepository<Attempts, Long>{
    List<Attempts> findBySessionUser(User user);
}
