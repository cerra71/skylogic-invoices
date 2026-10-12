package com.skylogic.invoice.advice;


public class ZipNonValidoException extends RuntimeException {

    public ZipNonValidoException(String message) {
        super(message);
    }

    public ZipNonValidoException(String message, Throwable cause) {
        super(message, cause);
    }
}
