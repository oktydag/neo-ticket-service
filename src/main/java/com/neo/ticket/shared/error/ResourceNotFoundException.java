package com.neo.ticket.shared.error;

import java.util.Map;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }

    public static ResourceNotFoundException of(ErrorCode errorCode, String resourceType, Object id) {
        return new ResourceNotFoundException(
                errorCode,
                "%s %s does not exist".formatted(resourceType, id),
                Map.of("resourceType", resourceType, "resourceId", String.valueOf(id)));
    }
}
