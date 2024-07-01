package ru.akazakov.beauty.application.expert.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.akazakov.beauty.application.expert.GetAllExpertsQuery;
import ru.akazakov.beauty.application.expert.port.GetAllExpertsPort;
import ru.akazakov.beauty.domain.expert.Expert;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class GetAllExpertsQueryImpl implements GetAllExpertsQuery {
    private final GetAllExpertsPort getAllExpertsPort;

    @Override
    public List<Expert> execute() {
        log.info("Get all experts");
        return getAllExpertsPort.getAll();
    }

}
