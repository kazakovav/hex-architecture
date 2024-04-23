package ru.akazakov.beauty.service.customer;

import ru.akazakov.beauty.domain.Customer;

public interface SaveCustomerRepository {
    void save(Customer customer);
}
