package com.yanajiki.application.bingoapp.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Predicate;

@EqualsAndHashCode(callSuper = false)
@Getter
public class Name extends AbstractVO<String> {

    private final String value;

    public Name(String value) {
        super(value, StringUtils::isNotBlank);
        throwVOExceptionIf("Name is empty.");
        
        this.value = value;
    }
}
