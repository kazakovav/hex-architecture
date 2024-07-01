package ru.akazakov.beauty.domain.service;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@ToString
@Jacksonized
@EqualsAndHashCode
@AllArgsConstructor
@RequiredArgsConstructor
public class ServiceItem {

    private final ServiceItemId id;

    private String name;

}
