package ru.akazakov.beauty.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class Employee {
    private final EmployeeId id;

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    public static class EmployeeId {

        private final UUID value;

        public static EmployeeId of(UUID value) {
            return new EmployeeId(value);
        }

        public static EmployeeId of(String value) {
            return of(UUID.fromString(value));
        }
    }
}
