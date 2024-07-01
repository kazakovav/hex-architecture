package ru.akazakov.beauty.infractructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.akazakov.beauty.application.expert.CreateExpertCommand;
import ru.akazakov.beauty.application.expert.GetAllExpertsQuery;
import ru.akazakov.beauty.application.expert.GetServiceItemsAvailableForExpertQuery;
import ru.akazakov.beauty.application.expert.impl.CreateExpertCommandImpl;
import ru.akazakov.beauty.application.expert.impl.GetAllExpertsQueryImpl;
import ru.akazakov.beauty.application.expert.impl.GetServiceItemsAvailableForExpertQueryImpl;
import ru.akazakov.beauty.application.expert.port.GetAllExpertsPort;
import ru.akazakov.beauty.application.expert.port.GetAllServiceItemsPort;
import ru.akazakov.beauty.application.expert.port.GetServiceItemsPort;
import ru.akazakov.beauty.application.expert.port.SaveExpertPort;

@Configuration
public class ExpertConfig {
    @Bean
    public CreateExpertCommand createExpertCommand(GetServiceItemsPort getServiceItemsPort,
                                                   SaveExpertPort saveExpertPort) {
        return new CreateExpertCommandImpl(getServiceItemsPort, saveExpertPort);
    }

    @Bean
    public GetAllExpertsQuery getAllExpertsQuery(GetAllExpertsPort getAllExpertsPort) {
        return new GetAllExpertsQueryImpl(getAllExpertsPort);
    }

    @Bean
    public GetServiceItemsAvailableForExpertQuery getServiceItemsAvailableForExpertQuery(
            GetAllServiceItemsPort getAllServiceItemsPort) {
        return new GetServiceItemsAvailableForExpertQueryImpl(getAllServiceItemsPort);
    }
}
