package com.neo.ticket.shared.web;

public final class RequestMetadataHolder {

    private static final ThreadLocal<RequestMetadata> CURRENT = new ThreadLocal<>();

    private RequestMetadataHolder() {
    }

    static void set(RequestMetadata metadata) {
        CURRENT.set(metadata);
    }

    static void clear() {
        CURRENT.remove();
    }

    public static RequestMetadata current() {
        RequestMetadata metadata = CURRENT.get();
        return metadata != null ? metadata : RequestMetadata.unknown();
    }
}
