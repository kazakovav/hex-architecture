package ru.akazakov.beauty.domain.expert;

import org.junit.jupiter.api.Test;
import ru.akazakov.beauty.domain.common.ContactInfo;
import ru.akazakov.beauty.domain.common.PersonalInfo;
import ru.akazakov.beauty.domain.common.TaxInfo;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ExpertTest {
    @Test
    public void testCreatedSuccessful() {
        PersonalInfo personalInfo = PersonalInfo.builder()
                .firstName("John")
                .lastName("Smith")
                .birthDate(LocalDate.of(1985, 11, 11))
                .build();
        ContactInfo contactInfo = ContactInfo.builder().phone("+79999999999").build();
        TaxInfo taxInfo = TaxInfo.builder().individualTaxpayerNumber("111-1111-1111").build();
        Expert expert = Expert.create(personalInfo, contactInfo, taxInfo);

        assertNotNull(expert.getContactInfo());
        assertNotNull(expert.getPersonalInfo());
        assertNotNull(expert.getTaxInfo());
    }

    @Test
    void testCreateFailedWithPhoneMustBotBeBlankError() {

        PersonalInfo personalInfo = PersonalInfo.builder()
                .firstName("John")
                .lastName("Smith")
                .birthDate(LocalDate.of(1985, 11, 11))
                .build();
        ContactInfo contactInfo = ContactInfo.builder().email("aaaa@aaa.aa").build();
        TaxInfo taxInfo = TaxInfo.builder().individualTaxpayerNumber("111-1111-1111").build();

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> {
            Expert.create(personalInfo, contactInfo, taxInfo);
        });

        assertEquals("[\"contactInfo.phone\" must not be blank]", illegalArgumentException.getMessage());
    }

    @Test
    void testCreateFailedPersonalInfoIsNullError() {
        ContactInfo contactInfo = ContactInfo.builder().phone("+79999999999").build();
        TaxInfo taxInfo = TaxInfo.builder().individualTaxpayerNumber("111-1111-1111").build();

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> {
            Expert.create(null, contactInfo, taxInfo);
        });

        assertEquals("[\"personalInfo\" must not be null]", illegalArgumentException.getMessage());
    }
}
