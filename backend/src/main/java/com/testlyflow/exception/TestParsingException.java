package com.testlyflow.exception;

import java.util.List;

public class TestParsingException extends RuntimeException {

    private final List<String> details;

    public TestParsingException(String message, List<String> details) {
        super(message);
        this.details = details;
    }

    public List<String> getDetails() {
        return details;
    }
}
