package ru.akazakov.beauty.web.expert;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.akazakov.beauty.application.expert.CreateExpertCommand;
import ru.akazakov.beauty.application.expert.GetAllExpertsQuery;
import ru.akazakov.beauty.application.expert.GetServiceItemsAvailableForExpertQuery;
import ru.akazakov.beauty.application.expert.request.CreateExpertCommandRequest;
import ru.akazakov.beauty.domain.expert.Expert;
import ru.akazakov.beauty.domain.service.ServiceItem;

import java.util.List;

@RestController
@RequestMapping("/api/v1/expert")
@RequiredArgsConstructor
public class ExpertController {

    private final CreateExpertCommand createExpertCommand;

    private final GetAllExpertsQuery getAllExpertsQuery;

    private final GetServiceItemsAvailableForExpertQuery getServiceItemsAvailableForExpertQuery;

    @PostMapping
    public Expert create(@RequestBody CreateExpertCommandRequest createExpertCommandRequest) {
        Expert result = createExpertCommand.execute(createExpertCommandRequest);
        return result;
    }

    @GetMapping("/list")
    public List<Expert> getAllExperts() {
        return getAllExpertsQuery.execute();
    }

    @GetMapping("/service-items")
    public List<ServiceItem> getAvailableServiceItems() {
        return getServiceItemsAvailableForExpertQuery.execute();
    }

}
