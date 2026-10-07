package com.skylogic.invoice.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite primary key for the Usage entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UsageId implements Serializable {

    private static final long serialVersionUID = 1L;
    private String billingAccountNumber;
    private String siteConnectivityId;
    private String date;
}