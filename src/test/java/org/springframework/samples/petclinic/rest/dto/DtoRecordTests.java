package org.springframework.samples.petclinic.rest.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DtoRecordTests {

    private Validator createValidator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return validator;
    }

    @Test
    void petRecordValidatesRequiredValuesAndBoundaries() {
        PetTypeDto type = new PetTypeDto("dog", 2);
        assertThrows(IllegalArgumentException.class, () ->
            new PetDto("x".repeat(31), LocalDate.now(), type, 1, 1, List.of()));
        assertThrows(IllegalArgumentException.class, () ->
            new PetDto("Rosy", LocalDate.now(), type, -1, 1, List.of()));
        assertThrows(IllegalArgumentException.class, () ->
            new PetDto("Rosy", LocalDate.now(), type, 1, -1, List.of()));
        assertThrows(NullPointerException.class, () ->
            new PetDto("Rosy", LocalDate.now(), type, 1, 1, null));

        PetDto missingRequiredValues = new PetDto(null, null, null, null, null, List.of());
        Set<ConstraintViolation<PetDto>> violations = createValidator().validate(missingRequiredValues);
        assertThat(violations)
            .extracting(violation -> violation.getPropertyPath().toString())
            .contains("name", "birthDate", "type");
    }

    @Test
    void visitRecordValidatesRequiredValuesAndBoundaries() {
        assertThrows(IllegalArgumentException.class, () ->
            new VisitDto(LocalDate.now(), "", 1, 2));
        assertThrows(IllegalArgumentException.class, () ->
            new VisitDto(LocalDate.now(), "x".repeat(256), 1, 2));
        assertThrows(IllegalArgumentException.class, () ->
            new VisitDto(LocalDate.now(), "checkup", -1, 2));
        assertThrows(IllegalArgumentException.class, () ->
            new VisitDto(LocalDate.now(), "checkup", 1, -1));

        VisitDto missingRequiredValues = new VisitDto(null, null, null, null);
        Set<ConstraintViolation<VisitDto>> violations = createValidator().validate(missingRequiredValues);
        assertThat(violations)
            .extracting(violation -> violation.getPropertyPath().toString())
            .contains("description", "petId");
    }
}
