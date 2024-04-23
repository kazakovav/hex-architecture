package ru.akazakov.beauty.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Customer {
    @Getter
    private final CustomerId id;

    @Getter
    private final String name;

    @Getter
    private final LocalDateTime created;

    public static Customer create(String name) {
        return new Customer(CustomerId.create(), name, LocalDateTime.now());
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CustomerId {

        private final UUID value;

        public static CustomerId of(UUID value) {
            return new CustomerId(value);
        }

        public static CustomerId of(String value) {
            return of(UUID.fromString(value));
        }

        public static CustomerId create() {
            return of(UUID.randomUUID());
        }
    }
}
