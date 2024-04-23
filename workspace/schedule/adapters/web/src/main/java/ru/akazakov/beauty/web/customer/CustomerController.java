package ru.akazakov.beauty.web.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.akazakov.beauty.domain.Customer;
import ru.akazakov.beauty.service.customer.CreateCustomerUseCase;
import ru.akazakov.beauty.service.customer.CreateCustomerRequest;
import ru.akazakov.beauty.web.customer.dto.CreateCustomerDto;
import ru.akazakov.beauty.web.customer.dto.CustomerDto;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerController {
    private final CreateCustomerUseCase createCustomerUseCase;

    @PostMapping
    public CustomerDto create(@RequestBody CreateCustomerDto createCustomerDto) {
        Customer customer = createCustomerUseCase.execute(new CreateCustomerRequest().setName(createCustomerDto.getName()));
        return new CustomerDto().setId(customer.getId().getValue()).setName(customer.getName()).setCreated(customer.getCreated());
    }

}
