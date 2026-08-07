package com.neo.ticket.iam.domain;

import com.neo.ticket.iam.domain.valueobject.Email;
import com.neo.ticket.shared.domain.valueobject.UserId;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    User save(User user);
}
