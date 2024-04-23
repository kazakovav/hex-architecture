package ru.akazakov.beauty.service.customer;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true)
public class CreateCustomerRequest {
    private String name;
}
