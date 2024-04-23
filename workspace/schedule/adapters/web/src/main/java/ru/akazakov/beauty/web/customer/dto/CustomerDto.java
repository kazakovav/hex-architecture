package ru.akazakov.beauty.web.customer.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class CustomerDto {
    private UUID id;
    private String name;
    private LocalDateTime created;
}
