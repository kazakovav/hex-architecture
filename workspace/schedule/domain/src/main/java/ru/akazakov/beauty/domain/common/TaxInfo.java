package ru.akazakov.beauty.domain.common;

import am.ik.yavi.builder.ValidatorBuilder;
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
public class TaxInfo {
    private String individualTaxpayerNumber;

    public static Validator<TaxInfo> validator = ValidatorBuilder.of(TaxInfo.class)
            ._string(TaxInfo::getIndividualTaxpayerNumber, TaxInfo.Fields.individualTaxpayerNumber, c -> c.notBlank())
            .build();
}
