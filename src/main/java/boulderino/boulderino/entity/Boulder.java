package boulderino.boulderino.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;

// Tracking von Progress an konkreten einzigartigen Bouldern
@Getter
@Setter
@Entity
public class Boulder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Attribute
    private String name;
    @Enumerated(EnumType.STRING)
    private Grade grade;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany( mappedBy = "boulder", cascade = CascadeType.ALL, orphanRemoval = true )
    @JsonIgnore
    private List<Attempts> attempts = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private WallAngle wallAngle;

    //TODO: GripType kann auch als Hashset realiziert werden um mehrere Grifftypen innerhalb einer Route zuzulassen.
    @Enumerated(EnumType.STRING)
    private GripType gripType;

    @Enumerated(EnumType.STRING)
    private RouteCharacter routeCharacter;

    @Enumerated(EnumType.STRING)
    private ClimbingStyle climbingStyle;

    private Boolean firstAscent = false;
    private LocalDate firstAscentDate;
    private Boolean flashed = false;

    public Boulder(){}

}
