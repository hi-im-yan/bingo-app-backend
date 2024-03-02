package com.yanajiki.application.bingoapp.domain.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@EqualsAndHashCode(callSuper = false)
@Getter
public class SessionCode extends AbstractVO<String> {

    private final String value;

    public SessionCode(String value) {
        super(value, StringUtils::isNotBlank);
        throwVOExceptionIf("SessionCode is empty.");
        this.value = value;
    }
}
