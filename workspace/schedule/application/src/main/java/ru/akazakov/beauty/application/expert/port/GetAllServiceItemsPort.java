package ru.akazakov.beauty.application.expert.port;

import ru.akazakov.beauty.domain.service.ServiceItem;

import java.util.List;

public interface GetAllServiceItemsPort {

    List<ServiceItem> getAll();

}
