package com.bombel.AccessControl.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByIdentifier(String identifier);
    Optional<User> findByIdentifier(String identifier);
}
