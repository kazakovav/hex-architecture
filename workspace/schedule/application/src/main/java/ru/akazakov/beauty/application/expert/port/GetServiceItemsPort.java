package ru.akazakov.beauty.application.expert.port;

import ru.akazakov.beauty.domain.service.ServiceItem;
import ru.akazakov.beauty.domain.service.ServiceItemId;

import java.util.Collection;
import java.util.Set;

public interface GetServiceItemsPort {

    Set<ServiceItem> getServiceItems(Collection<ServiceItemId> serviceItemsIds);

}
