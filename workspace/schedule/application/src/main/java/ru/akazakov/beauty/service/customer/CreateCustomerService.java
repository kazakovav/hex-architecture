package ru.akazakov.beauty.service.customer;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import ru.akazakov.beauty.domain.Customer;

@RequiredArgsConstructor
public class CreateCustomerService implements CreateCustomerUseCase {

    private final SaveCustomerRepository saveCustomerRepository;

    @Override
    @Transactional
    public Customer execute(CreateCustomerRequest request) {
        Customer customer = Customer.create(request.getName());
        saveCustomerRepository.save(customer);
        return customer;
    }
}
