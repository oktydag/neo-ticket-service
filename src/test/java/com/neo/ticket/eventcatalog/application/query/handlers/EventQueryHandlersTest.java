package com.neo.ticket.eventcatalog.application.query.handlers;

import com.neo.ticket.eventcatalog.application.EventCatalogQueries;
import com.neo.ticket.eventcatalog.application.EventView;
import com.neo.ticket.eventcatalog.application.query.EventSearchCriteria;
import com.neo.ticket.shared.domain.valueobject.Actor;
import com.neo.ticket.shared.domain.valueobject.Role;
import com.neo.ticket.shared.domain.valueobject.UserId;
import com.neo.ticket.shared.error.DomainException;
import com.neo.ticket.shared.error.ErrorCode;
import com.neo.ticket.shared.error.ForbiddenOperationException;
import com.neo.ticket.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.neo.ticket.testsupport.TestFixtures.customer;
import static com.neo.ticket.testsupport.TestFixtures.organizer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Event query handlers")
class EventQueryHandlersTest {

    private RecordingQueries queries;
    private ListEventsHandler listEventsHandler;
    private DiscoverEventsHandler discoverEventsHandler;
    private final Pageable pageable = PageRequest.of(0, 20);

    @BeforeEach
    void setUp() {
        queries = new RecordingQueries();
        listEventsHandler = new ListEventsHandler(queries);
        discoverEventsHandler = new DiscoverEventsHandler(queries);
    }

    @Test
    @DisplayName("given an administrator with no filter, when listing, then every event is queried")
    void administratorsSeeEverything() {
        listEventsHandler.handle(TestFixtures.admin(), null, EventSearchCriteria.unfiltered(), pageable);

        assertThat(queries.calls).containsExactly("findAll");
    }

    @Test
    @DisplayName("given an administrator filtering by owner, when listing, then that owner is queried")
    void administratorsMayFilterByOwner() {
        UserId someone = UserId.newId();

        listEventsHandler.handle(TestFixtures.admin(), someone, EventSearchCriteria.unfiltered(), pageable);

        assertThat(queries.calls).containsExactly("findOwnedBy");
        assertThat(queries.lastOwnerId).isEqualTo(someone);
    }

    @Test
    @DisplayName("given an organizer, when listing, then only their own events are queried")
    void organizersSeeTheirOwn() {
        UserId self = UserId.newId();

        listEventsHandler.handle(organizer(self), null, EventSearchCriteria.unfiltered(), pageable);

        assertThat(queries.calls).containsExactly("findOwnedBy");
        assertThat(queries.lastOwnerId).isEqualTo(self);
    }

    @Test
    @DisplayName("given an organizer asking for their own id, when listing, then it is allowed")
    void organizersMayNameThemselves() {
        UserId self = UserId.newId();

        listEventsHandler.handle(organizer(self), self, EventSearchCriteria.unfiltered(), pageable);

        assertThat(queries.lastOwnerId).isEqualTo(self);
    }

    @Test
    @DisplayName("given an organizer asking for another owner, when listing, then it is refused")
    void organizersCannotBrowseAnotherOwner() {
        Actor self = organizer(UserId.newId());

        assertThatThrownBy(() -> listEventsHandler.handle(self, UserId.newId(),
                EventSearchCriteria.unfiltered(), pageable))
                .isInstanceOf(ForbiddenOperationException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).errorCode())
                        .isEqualTo(ErrorCode.NOT_RESOURCE_OWNER));

        assertThat(queries.calls).as("nothing is fetched for a refused request").isEmpty();
    }

    @Test
    @DisplayName("given a customer, when listing, then only published events are queried")
    void customersSeePublishedEventsOnly() {
        listEventsHandler.handle(customer(UserId.newId()), null, EventSearchCriteria.unfiltered(), pageable);

        assertThat(queries.calls).containsExactly("findPublished");
    }

    @Test
    @DisplayName("given a customer naming an owner, when listing, then the filter cannot widen visibility")
    void customersCannotEscapeThePublishedFilter() {
        listEventsHandler.handle(customer(UserId.newId()), UserId.newId(),
                EventSearchCriteria.unfiltered(), pageable);

        assertThat(queries.calls).containsExactly("findPublished");
    }

    @Test
    @DisplayName("given someone holding both roles, when listing, then the wider organizer view applies")
    void organizerRoleWinsOverCustomer() {
        UserId self = UserId.newId();
        Actor both = new Actor(self, Set.of(Role.ORGANIZER, Role.CUSTOMER));

        listEventsHandler.handle(both, null, EventSearchCriteria.unfiltered(), pageable);

        assertThat(queries.calls).containsExactly("findOwnedBy");
    }

    @Test
    @DisplayName("given the discovery feed, when queried, then only published events are returned")
    void discoveryIsAlwaysPublishedOnly() {
        discoverEventsHandler.handle(EventSearchCriteria.unfiltered(), pageable);

        assertThat(queries.calls).containsExactly("findPublished");
    }

    @Test
    @DisplayName("given blank search text, when queried, then it is treated as no filter at all")
    void ignoresBlankSearchText() {
        assertThat(new EventSearchCriteria(null, null, "   ").searchText()).isEmpty();
        assertThat(new EventSearchCriteria(null, null, "jazz").searchText()).contains("jazz");
    }

    private static final class RecordingQueries implements EventCatalogQueries {

        private final List<String> calls = new ArrayList<>();
        private UserId lastOwnerId;

        @Override
        public Page<EventView> findPublished(EventSearchCriteria criteria, Pageable pageable) {
            calls.add("findPublished");
            return emptyPage(pageable);
        }

        @Override
        public Page<EventView> findOwnedBy(UserId ownerId, EventSearchCriteria criteria, Pageable pageable) {
            calls.add("findOwnedBy");
            lastOwnerId = ownerId;
            return emptyPage(pageable);
        }

        @Override
        public Page<EventView> findAll(EventSearchCriteria criteria, Pageable pageable) {
            calls.add("findAll");
            return emptyPage(pageable);
        }

        private static Page<EventView> emptyPage(Pageable pageable) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
    }
}
