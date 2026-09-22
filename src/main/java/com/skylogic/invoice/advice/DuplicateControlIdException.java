package com.skylogic.invoice.advice;


public class DuplicateControlIdException extends RuntimeException {

    public DuplicateControlIdException(String controlId) {
        super("Control ID already exists: " + controlId);
    }
}
