package ru.akazakov.beauty.application.expert.port;

import ru.akazakov.beauty.domain.expert.Expert;

import java.util.List;

public interface GetAllExpertsPort {

    List<Expert> getAll();

}
