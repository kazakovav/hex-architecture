package ru.akazakov.beauty.db.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.mapdb.serializer.GroupSerializerObjectArray;
import org.springframework.stereotype.Repository;
import ru.akazakov.beauty.application.expert.port.GetAllServiceItemsPort;
import ru.akazakov.beauty.application.expert.port.GetServiceItemsPort;
import ru.akazakov.beauty.domain.service.ServiceItem;
import ru.akazakov.beauty.domain.service.ServiceItemId;

import java.io.IOException;
import java.util.*;

@Repository
@Slf4j
public class GetServiceItemsAdapterInMemory implements GetServiceItemsPort, GetAllServiceItemsPort {
    private static final String DB_FILE_NAME = "services.db";
    private static final String MAP_NAME = "service_items";

    private ServiceItemSerialzer valueSerializer;

    public GetServiceItemsAdapterInMemory(ObjectMapper objectMapper) {
        this.valueSerializer = new ServiceItemSerialzer(objectMapper);
    }

    @PostConstruct
    void initialize() {
        log.info("Initializing service items");
        try (DB db = DBMaker.fileDB(DB_FILE_NAME).make()) {
            HTreeMap<UUID, ServiceItem> serviceItems = db.hashMap(MAP_NAME).keySerializer(Serializer.UUID)
                    .valueSerializer(valueSerializer).createOrOpen();

            if (CollectionUtils.isNotEmpty(serviceItems.getValues())) {
                log.info("Service items are contained in db: {}", new ArrayList<>(serviceItems.getValues()));
                return;
            }

            log.info("Creating new service items");

            UUID serviceId = UUID.randomUUID();
            serviceItems.put(serviceId, new ServiceItem(ServiceItemId.of(serviceId), "haircut"));

            serviceId = UUID.randomUUID();
            serviceItems.put(serviceId, new ServiceItem(ServiceItemId.of(serviceId), "manicure"));
        }
    }

    @Override
    public Set<ServiceItem> getServiceItems(Collection<ServiceItemId> serviceItemsIds) {
        log.info("Getting service items by ids: {}", serviceItemsIds);
        try (DB db = DBMaker.fileDB(DB_FILE_NAME).make()) {
            HTreeMap<UUID, ServiceItem> serviceItems = db.hashMap(MAP_NAME).keySerializer(Serializer.UUID)
                    .valueSerializer(valueSerializer).createOrOpen();

            Collection<ServiceItem> values = serviceItems.values().stream()
                    .filter(item -> serviceItemsIds.contains(item.getId())).toList();
            return new HashSet<ServiceItem>(values);
        }
    }

    @Override
    public List<ServiceItem> getAll() {
        log.info("Get all service items");
        try (DB db = DBMaker.fileDB(DB_FILE_NAME).make()) {
            HTreeMap<UUID, ServiceItem> serviceItems = db.hashMap(MAP_NAME).keySerializer(Serializer.UUID)
                    .valueSerializer(valueSerializer).createOrOpen();

            return new ArrayList<>(serviceItems.values());
        }
    }

    @RequiredArgsConstructor
    static class ServiceItemSerialzer extends GroupSerializerObjectArray<ServiceItem> {
        private final ObjectMapper objectMapper;

        @Override
        public void serialize(DataOutput2 dataOutput2, ServiceItem serviceItem) throws IOException {
            dataOutput2.writeUTF(objectMapper.writeValueAsString(serviceItem));
        }

        @Override
        public ServiceItem deserialize(DataInput2 dataInput2, int i) throws IOException {
            return objectMapper.readValue(dataInput2.readUTF(), ServiceItem.class);
        }
    }
}
