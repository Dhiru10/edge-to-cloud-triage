package com.triage.exception;

public class InvalidTransitionException extends RuntimeException {
    public InvalidTransitionException(String from, String to) {
        super("Cannot transition fault event from '" + from + "' to '" + to + "'");
    }
}
