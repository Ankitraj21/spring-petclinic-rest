package org.springframework.samples.petclinic.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class VisitBookingValidator {

    private final ClinicService clinicService;

    private final Clock clock;

    @Autowired
    public VisitBookingValidator(ClinicService clinicService) {
        this(clinicService, Clock.systemDefaultZone());
    }

    public VisitBookingValidator(ClinicService clinicService, Clock clock) {
        this.clinicService = clinicService;
        this.clock = clock;
    }

    public Pet validate(Integer petId, LocalDate date, Integer currentVisitId) {
        if (date != null && date.isBefore(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Visit date must not be in the past");
        }
        Pet pet = this.clinicService.findPetById(petId);
        if (pet == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pet does not exist");
        }
        if (date != null && this.clinicService.findVisitsByPetId(petId).stream()
            .anyMatch(visit -> date.equals(visit.getDate())
                && (currentVisitId == null || !Objects.equals(currentVisitId, visit.getId())))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A visit is already booked for this pet and date");
        }
        return pet;
    }
}
