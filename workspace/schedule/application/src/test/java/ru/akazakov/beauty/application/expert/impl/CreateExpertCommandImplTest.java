package ru.akazakov.beauty.application.expert.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.akazakov.beauty.application.expert.CreateExpertCommand;
import ru.akazakov.beauty.application.expert.port.GetServiceItemsPort;
import ru.akazakov.beauty.application.expert.port.SaveExpertPort;
import ru.akazakov.beauty.application.expert.request.CreateExpertCommandRequest;
import ru.akazakov.beauty.domain.expert.Expert;
import ru.akazakov.beauty.domain.service.ServiceItem;
import ru.akazakov.beauty.domain.service.ServiceItemId;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateExpertCommandImplTest {
    @Mock
    GetServiceItemsPort getServiceItemsPort;

    @Mock
    SaveExpertPort saveExpertPort;

    @Test
    void testExecuteSuccessAndServicesAreEmpty() {
        CreateExpertCommand createExpertCommand = new CreateExpertCommandImpl(getServiceItemsPort, saveExpertPort);

        List<UUID> serviceItemsIds = List.of(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"));

        List<ServiceItemId> ids = serviceItemsIds.stream().map(v -> ServiceItemId.of(v)).toList();

        CreateExpertCommandRequest request = CreateExpertCommandRequest.builder()
                .firstName("Test")
                .lastName("Test")
                .birthDate(LocalDate.of(1999, 01, 01))
                .phone("+78888888888")
                .individualTaxpayerNumber("123-123-123")
                .serviceItemsIds(serviceItemsIds)
                .build();

        when(getServiceItemsPort.getServiceItems(ids)).thenReturn(Collections.emptySet());

        doNothing().when(saveExpertPort).saveExpert(any());

        createExpertCommand.execute(request);

        verify(saveExpertPort, times(1)).saveExpert(argThat(arg -> {
            var expert = (Expert) arg;
            return CollectionUtils.isEmpty(expert.getServices())
                    && Objects.nonNull(expert.getId())
                    && Objects.nonNull(expert.getId().getValue());
        }));
    }

    @Test
    void testExecuteSuccessAndServicesAreNotEmpty() {
        CreateExpertCommand createExpertCommand = new CreateExpertCommandImpl(getServiceItemsPort, saveExpertPort);

        List<UUID> serviceItemsIds = List.of(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"));

        List<ServiceItemId> ids = serviceItemsIds.stream().map(v -> ServiceItemId.of(v)).toList();

        CreateExpertCommandRequest request = CreateExpertCommandRequest.builder()
                .firstName("Test")
                .lastName("Test")
                .birthDate(LocalDate.of(1999, 01, 01))
                .phone("+78888888888")
                .individualTaxpayerNumber("123-123-123")
                .serviceItemsIds(serviceItemsIds)
                .build();

        when(getServiceItemsPort.getServiceItems(ids)).thenReturn(Set.of(new ServiceItem(ids.get(0), "testService")));

        doNothing().when(saveExpertPort).saveExpert(any());

        createExpertCommand.execute(request);

        verify(saveExpertPort, times(1)).saveExpert(argThat(arg -> {
            var expert = (Expert) arg;
            return CollectionUtils.isNotEmpty(expert.getServices())
                    && Objects.nonNull(expert.getId())
                    && Objects.nonNull(expert.getId().getValue());
        }));
    }

    @Test
    void testExecuteFailedWithPhoneRequiredRequest() {
        CreateExpertCommand createExpertCommand = new CreateExpertCommandImpl(getServiceItemsPort, saveExpertPort);

        List<UUID> serviceItemsIds = List.of(UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6"));

        CreateExpertCommandRequest request = CreateExpertCommandRequest.builder()
                .firstName("Test")
                .lastName("Test")
                .birthDate(LocalDate.of(1999, 01, 01))
                .individualTaxpayerNumber("123-123-123")
                .serviceItemsIds(serviceItemsIds)
                .build();

        assertThrows(IllegalArgumentException.class, () -> createExpertCommand.execute(request));
    }
}
