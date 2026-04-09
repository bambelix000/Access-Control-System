package com.bombel.AccessControl.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByIdentifier(String identifier);
}
