package boulderino.boulderino.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Attempts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int tries;
    private boolean done;
    private boolean zone;

    @ManyToOne
    private Session session;

    @ManyToOne
    private Boulder boulder;

    public Attempts() {
    }

}
