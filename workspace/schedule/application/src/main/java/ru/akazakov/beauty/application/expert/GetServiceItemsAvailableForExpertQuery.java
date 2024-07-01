package ru.akazakov.beauty.application.expert;

import ru.akazakov.beauty.domain.service.ServiceItem;

import java.util.List;

public interface GetServiceItemsAvailableForExpertQuery {
    List<ServiceItem> execute();
}
