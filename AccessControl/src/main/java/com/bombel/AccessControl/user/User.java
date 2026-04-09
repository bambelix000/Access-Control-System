package com.bombel.AccessControl.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "user_seq", allocationSize = 1)
    private Long id;
    private String name;
    private String identifier;
    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    public User(String name, String identifier, UserRole userRole) {
        this.name = name;
        this.identifier = identifier;
        this.userRole = userRole;
    }
}
