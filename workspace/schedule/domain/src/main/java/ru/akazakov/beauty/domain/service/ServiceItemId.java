package ru.akazakov.beauty.domain.service;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Builder
@ToString
@Jacksonized
@EqualsAndHashCode
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceItemId {
    private final UUID value;

    public static ServiceItemId of(UUID value) {
        return new ServiceItemId(value);
    }

    public static ServiceItemId create() {
        return of(UUID.randomUUID());
    }
}
