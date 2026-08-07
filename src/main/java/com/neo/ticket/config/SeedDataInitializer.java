package com.neo.ticket.config;

import com.neo.ticket.iam.domain.PasswordHasher;
import com.neo.ticket.iam.domain.User;
import com.neo.ticket.iam.domain.UserRepository;
import com.neo.ticket.iam.domain.valueobject.Email;
import com.neo.ticket.iam.domain.valueobject.RawPassword;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.domain.valueobject.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Set;

@Configuration
@ConditionalOnProperty(prefix = "neo.seed", name = "enabled", havingValue = "true")
class SeedDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(SeedDataInitializer.class);

    private static final List<SeedAccount> ACCOUNTS = List.of(
            new SeedAccount("admin@neo.io", Set.of(Role.ADMIN)),
            new SeedAccount("organizer@neo.io", Set.of(Role.ORGANIZER)),
            new SeedAccount("customer@neo.io", Set.of(Role.CUSTOMER)));

    @Bean
    ApplicationRunner seedUsers(UserRepository users, PasswordHasher passwordHasher,
                                SeedDataProperties properties, Clock clock) {
        return arguments -> createMissingAccounts(users, passwordHasher, properties, clock);
    }

    @Transactional
    void createMissingAccounts(UserRepository users, PasswordHasher passwordHasher,
                               SeedDataProperties properties, Clock clock) {
        RawPassword password = RawPassword.of(properties.password());
        int created = 0;

        for (SeedAccount account : ACCOUNTS) {
            Email email = Email.of(account.email());
            if (users.existsByEmail(email)) {
                continue;
            }
            users.save(User.provision(
                    UserId.newId(), email, passwordHasher.hash(password),
                    account.roles(), clock.instant()));
            created++;
        }

        if (created > 0) {
            log.warn("Seeded {} demo account(s) with a shared, publicly documented password. "
                    + "Never enable neo.seed outside development.", created);
        }
    }

    private record SeedAccount(String email, Set<Role> roles) {
    }
}
