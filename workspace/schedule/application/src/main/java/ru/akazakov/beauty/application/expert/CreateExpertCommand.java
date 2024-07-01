package ru.akazakov.beauty.application.expert;

import ru.akazakov.beauty.application.expert.request.CreateExpertCommandRequest;
import ru.akazakov.beauty.domain.expert.Expert;

public interface CreateExpertCommand {
    Expert execute(CreateExpertCommandRequest request);
}
