package boulderino.boulderino.repository;

import boulderino.boulderino.entity.Boulder;
import boulderino.boulderino.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface BoulderRepository extends JpaRepository<Boulder, Long> {

    List<Boulder> findByUser(User user);
}
