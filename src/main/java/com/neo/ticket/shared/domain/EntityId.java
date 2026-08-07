package com.neo.ticket.shared.domain;

import java.io.Serializable;
import java.util.UUID;

public interface EntityId extends Serializable {

    UUID value();
}
