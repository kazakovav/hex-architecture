package ru.akazakov.beauty.db.customer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ru.akazakov.beauty.domain.Customer;
import ru.akazakov.beauty.service.customer.SaveCustomerRepository;

@Repository
@Slf4j
public class SaveCustomerRepositoryStub implements SaveCustomerRepository {
    @Override
    public void save(Customer customer) {
        log.info("Saving customer");
    }
}
