package ru.akazakov.beauty.application.expert.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.akazakov.beauty.application.expert.GetServiceItemsAvailableForExpertQuery;
import ru.akazakov.beauty.application.expert.port.GetAllServiceItemsPort;
import ru.akazakov.beauty.domain.service.ServiceItem;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class GetServiceItemsAvailableForExpertQueryImpl implements GetServiceItemsAvailableForExpertQuery {
    private final GetAllServiceItemsPort getAllServiceItemsPort;

    @Override
    public List<ServiceItem> execute() {
        log.info("Get all service items");
        return getAllServiceItemsPort.getAll();
    }

}
