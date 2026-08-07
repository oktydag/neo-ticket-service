package com.neo.ticket.iam.infrastructure.persistence;

import com.neo.ticket.iam.domain.valueobject.Email;
import com.neo.ticket.iam.domain.valueobject.PasswordHash;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public final class IamConverters {

    private IamConverters() {
    }

    @Converter(autoApply = true)
    public static class EmailConverter implements AttributeConverter<Email, String> {

        @Override
        public String convertToDatabaseColumn(Email attribute) {
            return attribute != null ? attribute.value() : null;
        }

        @Override
        public Email convertToEntityAttribute(String column) {
            return column != null ? new Email(column) : null;
        }
    }

    @Converter(autoApply = true)
    public static class PasswordHashConverter implements AttributeConverter<PasswordHash, String> {

        @Override
        public String convertToDatabaseColumn(PasswordHash attribute) {
            return attribute != null ? attribute.value() : null;
        }

        @Override
        public PasswordHash convertToEntityAttribute(String column) {
            return column != null ? new PasswordHash(column) : null;
        }
    }
}
