package com.xshopai.orderprocessor.model.events;

import lombok.Data;

/**
 * Address event data
 * Matches the schema from the .NET Order Service
 */
@Data
public class AddressEvent {
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}
