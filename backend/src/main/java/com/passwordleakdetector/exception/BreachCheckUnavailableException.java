package com.passwordleakdetector.exception;

public class BreachCheckUnavailableException extends RuntimeException {

    public BreachCheckUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
