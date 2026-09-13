package boulderino.boulderino.entity;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

//Ein Attempt ist ein zusammengefasster Boulder-Versuch bzw.
// eine zusammenhängende Versuchseinheit eines Users auf einem Boulder innerhalb einer Session.
// Tracking von Trainingsvolumen
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

    @CreationTimestamp
    @Column(updatable = true) //TODO: auf false setzen
    private LocalDateTime createdAt;

    public Attempts() {
    }

}
