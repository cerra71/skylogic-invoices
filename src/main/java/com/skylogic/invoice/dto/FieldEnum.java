package com.skylogic.invoice.dto;


public enum FieldEnum {
    Field1("Field1"),
    Field2("Field2"),
    Field3("Field3"),
    invoiceNumber("invoiceNumber");

    private final String value;

    FieldEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
