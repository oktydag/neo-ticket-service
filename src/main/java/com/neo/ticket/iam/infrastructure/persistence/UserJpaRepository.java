package com.neo.ticket.iam.infrastructure.persistence;

import com.neo.ticket.iam.domain.User;
import com.neo.ticket.iam.domain.valueobject.Email;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);
}
