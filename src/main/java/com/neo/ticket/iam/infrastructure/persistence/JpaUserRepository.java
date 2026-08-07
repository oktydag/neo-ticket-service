package com.neo.ticket.iam.infrastructure.persistence;

import com.neo.ticket.iam.domain.User;
import com.neo.ticket.iam.domain.UserRepository;
import com.neo.ticket.iam.domain.valueobject.Email;
import com.neo.ticket.shared.domain.valueobject.UserId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaUserRepository implements UserRepository {

    private final UserJpaRepository jpaRepository;

    JpaUserRepository(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpaRepository.findById(id.value());
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        return jpaRepository.save(user);
    }
}
