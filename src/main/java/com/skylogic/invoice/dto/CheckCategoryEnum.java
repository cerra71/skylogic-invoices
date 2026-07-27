package com.skylogic.invoice.dto;


public enum CheckCategoryEnum {

	valueCheck("valueCheck"),
	coherenceCheck("coherenceCheck"),
	mismatchSourceCheck("mismatchSourceCheck"),
	calculateCheck("calculateCheck");

    private final String value;

    CheckCategoryEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
