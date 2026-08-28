package boulderino.repository;

import boulderino.boulderino.entity.Boulder;
import org.springframework.data.jpa.repository.JpaRepository;


public interface BoulderRepository extends JpaRepository<Boulder, Long> {

}
