package com.yanajiki.application.bingoapp.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;
import java.util.function.Predicate;

@EqualsAndHashCode(callSuper = false)
@Getter
public class Id extends AbstractVO<String> {

    private final String value;

    public Id(String value) {
        super(value, isValidUUID(value));
        throwVOExceptionIf("Id is empty or not valid.");
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
