package org.springframework.samples.petclinic.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.samples.petclinic.rest.validation.PetAgeValidation;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * A pet and its associated read-only details.
 *
 * @param name the pet's name
 * @param birthDate the pet's birth date
 * @param type the pet's type
 * @param id the pet's identifier
 * @param ownerId the owner's identifier
 * @param visits the pet's visits
 */
@Schema(name = "Pet", description = "A pet.")
@JsonTypeName("Pet")
public record PetDto(
    @NotNull @Size(max = MAX_NAME_LENGTH) @JsonProperty("name") String name,
    @NotNull @Valid @PetAgeValidation
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonProperty("birthDate") LocalDate birthDate,
    @NotNull @Valid @JsonProperty("type") PetTypeDto type,
    @Nullable @Min(0) @JsonProperty("id") Integer id,
    @Nullable @Min(0) @JsonProperty("ownerId") Integer ownerId,
    @NotNull @Valid @JsonProperty("visits") List<VisitDto> visits
) {
    /** Maximum length of the pet's name. */
    private static final int MAX_NAME_LENGTH = 30;

    /**
     * Validates record invariants and defensively copies visits.
     */
    public PetDto {
        if (name != null && name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                "name must be at most 30 characters");
        }
        if (id != null && id < 0) {
            throw new IllegalArgumentException("id must be non-negative");
        }
        if (ownerId != null && ownerId < 0) {
            throw new IllegalArgumentException("ownerId must be non-negative");
        }
        visits = List.copyOf(
            Objects.requireNonNull(visits, "visits must not be null"));
    }
}
