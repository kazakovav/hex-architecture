package ru.akazakov.beauty.domain.common;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.constraint.CharSequenceConstraint;
import am.ik.yavi.core.Validator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Getter
@Builder
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
public class ContactInfo {
    private String phone;
    private String email;

    public static Validator<ContactInfo> requirePhoneValidator = ValidatorBuilder.of(ContactInfo.class)
            ._string(ContactInfo::getPhone, ContactInfo.Fields.phone, CharSequenceConstraint::notBlank)
            .build();

    public static Validator<ContactInfo> requireEmailValidator = ValidatorBuilder.of(ContactInfo.class)
            ._string(ContactInfo::getEmail, ContactInfo.Fields.email, c -> c.notBlank().email())
            .build();
}
