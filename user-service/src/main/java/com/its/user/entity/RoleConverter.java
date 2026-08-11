package com.its.user.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link Role} to the {@code TINYINT} column and back.
 *
 * <p>This class is the single translation point between the schema's integer and the
 * application's enum (SRS A-04). Deliberately not {@code @Converter(autoApply = true)}:
 * the conversion is declared explicitly on the field in {@link User}, so a reader looking
 * at the entity can see that the column is an integer without having to know this class
 * exists.
 *
 * <p>Note that a plain {@code @Enumerated(EnumType.ORDINAL)} would have produced the
 * right numbers here purely by accident of declaration order - and would silently write
 * the wrong ones the moment a third role were added anywhere but the end of the enum.
 */
@Converter
public class RoleConverter implements AttributeConverter<Role, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Role role) {
        return role == null ? null : role.getCode();
    }

    @Override
    public Role convertToEntityAttribute(Integer code) {
        return code == null ? null : Role.fromCode(code);
    }
}
