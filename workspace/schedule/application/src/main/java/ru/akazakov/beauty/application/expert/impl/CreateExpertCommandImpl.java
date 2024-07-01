package ru.akazakov.beauty.application.expert.impl;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.constraint.CharSequenceConstraint;
import am.ik.yavi.core.ConstraintViolation;
import am.ik.yavi.core.ConstraintViolations;
import am.ik.yavi.core.Validator;
import am.ik.yavi.message.SimpleMessageFormatter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import ru.akazakov.beauty.application.expert.CreateExpertCommand;
import ru.akazakov.beauty.application.expert.port.GetServiceItemsPort;
import ru.akazakov.beauty.application.expert.port.SaveExpertPort;
import ru.akazakov.beauty.application.expert.request.CreateExpertCommandRequest;
import ru.akazakov.beauty.domain.common.ContactInfo;
import ru.akazakov.beauty.domain.common.PersonalInfo;
import ru.akazakov.beauty.domain.common.TaxInfo;
import ru.akazakov.beauty.domain.expert.Expert;
import ru.akazakov.beauty.domain.service.ServiceItem;
import ru.akazakov.beauty.domain.service.ServiceItemId;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class CreateExpertCommandImpl implements CreateExpertCommand {
    private final GetServiceItemsPort getServiceItemsPort;
    private final SaveExpertPort saveExpertPort;

    public static Validator<CreateExpertCommandRequest> validator = ValidatorBuilder.<CreateExpertCommandRequest>of()
            .constraint(CreateExpertCommandRequest::getFirstName, CreateExpertCommandRequest.Fields.firstName,
                    c -> c.notBlank())
            .constraint(CreateExpertCommandRequest::getLastName, CreateExpertCommandRequest.Fields.lastName,
                    c -> c.notBlank())
            ._object(CreateExpertCommandRequest::getBirthDate, CreateExpertCommandRequest.Fields.birthDate,
                    c -> c.notNull())
            ._string(CreateExpertCommandRequest::getPhone, CreateExpertCommandRequest.Fields.phone,
                    CharSequenceConstraint::notBlank)
            ._string(CreateExpertCommandRequest::getIndividualTaxpayerNumber,
                    CreateExpertCommandRequest.Fields.individualTaxpayerNumber, CharSequenceConstraint::notBlank)
            .messageFormatter(new SimpleMessageFormatter())
            .build();

    @Override
    @Transactional
    public Expert execute(CreateExpertCommandRequest request) {
        log.info("Create expert with request: {}", request);

        ConstraintViolations violations = validator.validate(request);
        if (!violations.isValid()) {
            log.error("Validation error: {}", violations);
            throw new IllegalArgumentException(
                    violations.stream().map(ConstraintViolation::message).toList().toString());
        }

        PersonalInfo personalInfo = buildPersonalInfo(request);
        ContactInfo contactInfo = buildContactInfo(request);
        TaxInfo taxInfo = buildTaxInfo(request);

        Expert expert = Expert.create(personalInfo, contactInfo, taxInfo);

        Set<ServiceItem> serviceItems = getServiceItems(request.getServiceItemsIds());

        if (CollectionUtils.isNotEmpty(serviceItems)) {
            log.info("Assigning service items to expert: {}", serviceItems);
            expert.assignServiceItems(serviceItems);
        }

        log.info("Save expert: {}", expert);
        saveExpertPort.saveExpert(expert);

        return expert;
    }

    private Set<ServiceItem> getServiceItems(List<UUID> itemsUUIDs) {
        List<ServiceItemId> serviceItemsIds = itemsUUIDs.stream().map(id -> ServiceItemId.of(id))
                .toList();
        return getServiceItemsPort.getServiceItems(serviceItemsIds);
    }

    private TaxInfo buildTaxInfo(CreateExpertCommandRequest request) {
        TaxInfo taxInfo = TaxInfo.builder().individualTaxpayerNumber(request.getIndividualTaxpayerNumber()).build();
        return taxInfo;
    }

    private ContactInfo buildContactInfo(CreateExpertCommandRequest request) {
        ContactInfo contactInfo = ContactInfo.builder()
                .phone(request.getPhone())
                .email(request.getEmail())
                .build();
        return contactInfo;
    }

    private PersonalInfo buildPersonalInfo(CreateExpertCommandRequest request) {
        return PersonalInfo.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .middleName(request.getMiddleName())
                .birthDate(request.getBirthDate())
                .build();
    }

}
