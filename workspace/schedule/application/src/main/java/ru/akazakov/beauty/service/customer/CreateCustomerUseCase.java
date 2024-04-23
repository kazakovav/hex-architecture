package ru.akazakov.beauty.service.customer;

import ru.akazakov.beauty.domain.Customer;

public interface CreateCustomerUseCase {
    Customer execute(CreateCustomerRequest request);
}
