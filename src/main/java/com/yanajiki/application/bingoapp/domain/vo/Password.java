package com.yanajiki.application.bingoapp.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@EqualsAndHashCode(callSuper = false)
@Getter
public class Password extends AbstractVO<String> {

    private final String value;

    public Password(String value) {
        super(value, StringUtils::isNotBlank);
        throwVOExceptionIf("Password is empty.");
        this.value = value;
    }
}
