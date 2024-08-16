package ru.akazakov.beauty.web.expert;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.akazakov.beauty.application.expert.CreateExpertCommand;
import ru.akazakov.beauty.application.expert.GetAllExpertsQuery;
import ru.akazakov.beauty.application.expert.GetServiceItemsAvailableForExpertQuery;
import ru.akazakov.beauty.domain.expert.Expert;
import ru.akazakov.beauty.domain.service.ServiceItem;
import ru.akazakov.beauty.web.api.ExpertApi;
import ru.akazakov.beauty.web.api.model.CreateExpertCommandRequestDto;
import ru.akazakov.beauty.web.api.model.ExpertResponseDto;
import ru.akazakov.beauty.web.api.model.ServiceItemDto;
import ru.akazakov.beauty.web.expert.mapper.ExpertMapper;

import java.util.List;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class ExpertController implements ExpertApi {

    private final CreateExpertCommand createExpertCommand;

    private final GetAllExpertsQuery getAllExpertsQuery;

    private final GetServiceItemsAvailableForExpertQuery getServiceItemsAvailableForExpertQuery;

    private final ExpertMapper expertMapper;

    @Override
    public ResponseEntity<ExpertResponseDto> create(
            @Valid CreateExpertCommandRequestDto createExpertCommandRequestDto) {
        Expert expert = createExpertCommand
                .execute(expertMapper.toCreateExpertCommandRequest(createExpertCommandRequestDto));

        ExpertResponseDto result = expertMapper.toExpertResponseDto(expert);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<List<ExpertResponseDto>> getAllExperts() {
        List<Expert> experts = getAllExpertsQuery.execute();
        List<ExpertResponseDto> result = expertMapper.toExpertResponseDto(experts);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<List<ServiceItemDto>> getAvailableServiceItems() {
        List<ServiceItem> serviceItems = getServiceItemsAvailableForExpertQuery.execute();
        List<ServiceItemDto> result = expertMapper.toServiceItemResponseDto(serviceItems);
        return ResponseEntity.ok(result);
    }

}
