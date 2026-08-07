package com.neo.ticket.eventcatalog.application.command;

import java.time.Instant;

public record UpdateEventCommand(String title, String venue, Instant startsAt, Instant endsAt, int capacity) {
}
