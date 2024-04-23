package ru.akazakov.beauty.infractructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.akazakov.beauty.service.customer.CreateCustomerService;
import ru.akazakov.beauty.service.customer.CreateCustomerUseCase;
import ru.akazakov.beauty.service.customer.SaveCustomerRepository;

@Configuration
public class CustomerServiceConfig {

    @Bean
    public CreateCustomerUseCase createCustomerUseCase(SaveCustomerRepository saveCustomerRepository) {
        return new CreateCustomerService(saveCustomerRepository);
    }
}
