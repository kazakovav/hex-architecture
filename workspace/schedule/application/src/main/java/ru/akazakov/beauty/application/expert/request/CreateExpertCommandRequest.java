package ru.akazakov.beauty.application.expert.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@Jacksonized
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
public class CreateExpertCommandRequest {
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate birthDate;

    private String phone;
    private String email;

    private String individualTaxpayerNumber;

    private List<UUID> serviceItemsIds;

}
