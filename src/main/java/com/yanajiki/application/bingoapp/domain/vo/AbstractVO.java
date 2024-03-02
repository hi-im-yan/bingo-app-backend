package com.yanajiki.application.bingoapp.domain.vo;

import com.yanajiki.application.bingoapp.domain.exception.VOException;
import lombok.EqualsAndHashCode;

import java.util.function.Predicate;

public class AbstractVO<T> {

    private final T value;

    private final Predicate<T> predicate;

    public AbstractVO(T value, Predicate<T> predicate) {
        this.value = value;
        this.predicate = predicate;
    }

    public T getValue() {
        return value;
    }

    protected void throwVOExceptionIf(String exceptionMessage) {
        if (!this.predicate.test(this.value)) {
            throw new VOException(exceptionMessage);
        }
    }
}
