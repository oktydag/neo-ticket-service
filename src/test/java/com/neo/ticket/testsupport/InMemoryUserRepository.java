package com.neo.ticket.testsupport;

import com.neo.ticket.iam.domain.User;
import com.neo.ticket.iam.domain.UserRepository;
import com.neo.ticket.iam.domain.valueobject.Email;
import com.neo.ticket.shared.domain.valueobject.UserId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryUserRepository implements UserRepository {

    private final Map<UserId, User> byId = new LinkedHashMap<>();

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return byId.values().stream().filter(user -> user.email().equals(email)).findFirst();
    }

    @Override
    public boolean existsByEmail(Email email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public User save(User user) {
        byId.put(user.id(), user);
        return user;
    }

    public int count() {
        return byId.size();
    }
}
