package ru.akazakov.beauty.domain.expert;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Builder
@ToString
@Jacksonized
@EqualsAndHashCode
@RequiredArgsConstructor
public class ExpertId {
    private final UUID value;

    public static ExpertId create() {
        return new ExpertId(UUID.randomUUID());
    }

}
