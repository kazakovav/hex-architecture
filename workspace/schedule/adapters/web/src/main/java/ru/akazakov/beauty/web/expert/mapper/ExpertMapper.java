package ru.akazakov.beauty.web.expert.mapper;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.akazakov.beauty.application.expert.request.CreateExpertCommandRequest;
import ru.akazakov.beauty.domain.expert.Expert;
import ru.akazakov.beauty.domain.expert.ExpertId;
import ru.akazakov.beauty.domain.service.ServiceItem;
import ru.akazakov.beauty.domain.service.ServiceItemId;
import ru.akazakov.beauty.web.api.model.CreateExpertCommandRequestDto;
import ru.akazakov.beauty.web.api.model.ExpertResponseDto;
import ru.akazakov.beauty.web.api.model.ServiceItemDto;

@Mapper
public interface ExpertMapper {

    CreateExpertCommandRequest toCreateExpertCommandRequest(
            CreateExpertCommandRequestDto createExpertCommandRequestDto);

    @Mapping(target = "firstName", source = "personalInfo.firstName")
    @Mapping(target = "middleName", source = "personalInfo.middleName")
    @Mapping(target = "lastName", source = "personalInfo.lastName")
    @Mapping(target = "phone", source = "contactInfo.phone")
    @Mapping(target = "email", source = "contactInfo.email")
    ExpertResponseDto toExpertResponseDto(Expert expert);

    List<ExpertResponseDto> toExpertResponseDto(List<Expert> experts);

    List<ServiceItemDto> toServiceItemResponseDto(List<ServiceItem> serviceItems);

    default UUID map(ExpertId expertId) {
        return expertId.getValue();
    }

    default UUID map(ServiceItemId serviceItemId) {
        return serviceItemId.getValue();
    }

}
