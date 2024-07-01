package ru.akazakov.beauty.application.expert;

import ru.akazakov.beauty.domain.expert.Expert;

import java.util.List;

public interface GetAllExpertsQuery {

    List<Expert> execute();
}
