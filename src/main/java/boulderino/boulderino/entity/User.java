package boulderino.boulderino.entity;

import java.util.ArrayList;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;


@Getter
@Setter
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Attribute
    private String name;
    //TODO: @JsonIgnore
    private String password;
    
    @Column(nullable = false, unique=true)
    private String email;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Session> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Boulder> boulders = new ArrayList<>();

    public User(){}

}
