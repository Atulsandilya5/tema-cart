package com.temacart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PlaceOrderRequest {

    @NotBlank(message = "Shipping name is required")
    @Size(max = 100, message = "Name must be under 100 characters")
    private String shippingName;

    @NotBlank(message = "Shipping phone is required")
    @Size(max = 15, message = "Phone must be under 15 characters")
    private String shippingPhone;

    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Address must be under 500 characters")
    private String shippingAddress;

    public PlaceOrderRequest() {
    }

    public PlaceOrderRequest(String shippingName, String shippingPhone, String shippingAddress) {
        this.shippingName = shippingName;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
    }

    public String getShippingName() { return shippingName; }
    public void setShippingName(String shippingName) { this.shippingName = shippingName; }

    public String getShippingPhone() { return shippingPhone; }
    public void setShippingPhone(String shippingPhone) { this.shippingPhone = shippingPhone; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
}