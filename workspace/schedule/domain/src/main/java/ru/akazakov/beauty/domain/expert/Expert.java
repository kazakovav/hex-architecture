package ru.akazakov.beauty.domain.expert;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.constraint.ObjectConstraint;
import am.ik.yavi.core.ConstraintViolation;
import am.ik.yavi.core.ConstraintViolations;
import am.ik.yavi.core.Validator;
import am.ik.yavi.message.SimpleMessageFormatter;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import ru.akazakov.beauty.domain.common.ContactInfo;
import ru.akazakov.beauty.domain.common.PersonalInfo;
import ru.akazakov.beauty.domain.common.TaxInfo;
import ru.akazakov.beauty.domain.service.ServiceItem;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Getter
@Builder
@ToString
@Jacksonized
@EqualsAndHashCode
@FieldNameConstants
@RequiredArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Expert {
    private final ExpertId id;

    private PersonalInfo personalInfo;

    private ContactInfo contactInfo;

    private TaxInfo taxInfo;

    private Set<ServiceItem> services;

    public static Expert create(PersonalInfo personalInfo, ContactInfo contactInfo, TaxInfo taxInfo) {
        log.info("Creating new Expert, personalInfo: {}, contactInfo: {}, taxInfo: {}", personalInfo, contactInfo,
                taxInfo);
        return builder()
                .id(ExpertId.create())
                .personalInfo(personalInfo)
                .contactInfo(contactInfo)
                .taxInfo(taxInfo)
                .build();
    }

    public static Expert create(PersonalInfo personalInfo, ContactInfo contactInfo) {
        log.info("Creating new Expert, personalInfo: {}, contactInfo: {}", personalInfo, contactInfo);
        return builder()
                .id(ExpertId.create())
                .personalInfo(personalInfo)
                .contactInfo(contactInfo)
                .build();
    }

    public void assignServiceItems(Collection<ServiceItem> serviceItems) {
        if (services == null) {
            services = new HashSet<>();
        }
        services.addAll(serviceItems);
        log.info("New service items are added to expert, expertId: {}, serviceItems: {}", this.id, serviceItems);
    }

    public static ExpertBuilder builder() {
        return new ExpertBuilderWithValidation();
    }

    private static class ExpertBuilderWithValidation extends ExpertBuilder {

        @Override
        public Expert build() {
            Expert expert = super.build();
            ConstraintViolations violations = validator.validate(expert);
            if (!violations.isValid()) {
                throw new IllegalArgumentException(
                        violations.stream().map(ConstraintViolation::message).toList().toString());
            }
            return expert;
        }

    }

    public static Validator<Expert> validator = ValidatorBuilder.<Expert>of()
            ._object(Expert::getId, Fields.id, c -> c.notNull())
            ._object(Expert::getPersonalInfo, Fields.personalInfo, ObjectConstraint::notNull)
            .nestIfPresent(Expert::getPersonalInfo, Fields.personalInfo,
                    PersonalInfo.requireNameAndBirthDateValidator)
            ._object(Expert::getContactInfo, Fields.contactInfo, ObjectConstraint::notNull)
            .nestIfPresent(Expert::getContactInfo, Fields.contactInfo, ContactInfo.requirePhoneValidator)
            .nestIfPresent(Expert::getTaxInfo, Fields.taxInfo, TaxInfo.validator)
            .messageFormatter(new SimpleMessageFormatter())
            .build();

}
