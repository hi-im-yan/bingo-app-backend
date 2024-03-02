package com.yanajiki.application.bingoapp.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;
import java.util.function.Predicate;

@EqualsAndHashCode(callSuper = false)
@Getter
public class CreatorHash extends AbstractVO<String> {

    private final String value;

    public CreatorHash(String value) {
        super(value, isValidUUID(value));
        throwVOExceptionIf("CreatorHash is empty or not valid.");
        this.value = value;
    }

    private static Predicate<String> isValidUUID(String value) {
        return predicate -> {
            try {
                UUID.fromString(value);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        };
    }
}
