package org.elyashevich.producer.api.dto.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class ExceptionBodyDto {

    private final String message;
    private final Map<String, String> errors;

    public ExceptionBodyDto(String message) {
        this.message = message;
        this.errors = new HashMap<>();
    }
}