package com.api.gateway.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@Setter
@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class UnAuthorizedException extends RuntimeException{
    private final String resourceName;
    private final String fieldName;
    private final transient Object fieldValue;

    public UnAuthorizedException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public UnAuthorizedException(String message) {
        super(message);
        this.resourceName = "";
        this.fieldName = "";
        this.fieldValue = "";
    }
}
