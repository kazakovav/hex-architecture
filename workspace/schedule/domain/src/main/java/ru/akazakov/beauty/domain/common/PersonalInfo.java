package ru.akazakov.beauty.domain.common;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDate;

@Getter
@Builder
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
public class PersonalInfo {
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate birthDate;

    public static Validator<PersonalInfo> requireNameAndBirthDateValidator = ValidatorBuilder.<PersonalInfo>of()
            .constraint(PersonalInfo::getFirstName, PersonalInfo.Fields.firstName, c -> c.notBlank())
            .constraint(PersonalInfo::getLastName, PersonalInfo.Fields.lastName, c -> c.notBlank())
            ._object(PersonalInfo::getBirthDate, PersonalInfo.Fields.birthDate, c -> c.notNull())
            .build();

    public static Validator<PersonalInfo> requireAllFieldsValidator = ValidatorBuilder.<PersonalInfo>of()
            .constraint(PersonalInfo::getFirstName, PersonalInfo.Fields.firstName, c -> c.notBlank())
            .constraint(PersonalInfo::getMiddleName, PersonalInfo.Fields.middleName, c -> c.notBlank())
            .constraint(PersonalInfo::getLastName, PersonalInfo.Fields.lastName, c -> c.notBlank())
            ._object(PersonalInfo::getBirthDate, PersonalInfo.Fields.birthDate, c -> c.notNull())
            .build();
}
