package org.springframework.samples.petclinic.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * A vet visit associated with a pet.
 *
 * @param date the date of the visit
 * @param description a description of the visit
 * @param id the visit's identifier
 * @param petId the identifier of the associated pet
 */
@Schema(name = "Visit", description = "A booking for a vet visit.")
@JsonTypeName("Visit")
public record VisitDto(
    @Nullable
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonProperty("date") LocalDate date,
    @NotNull @Size(min = 1, max = MAX_DESCRIPTION_LENGTH)
    @JsonProperty("description") String description,
    @Nullable @Min(0) @JsonProperty("id") Integer id,
    @NotNull @Min(0) @JsonProperty("petId") Integer petId
) {
    /** Maximum length of a visit description. */
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    /**
     * Validates the description and identifier boundaries.
     */
    public VisitDto {
        if (description != null && (description.isEmpty()
            || description.length() > MAX_DESCRIPTION_LENGTH)) {
            throw new IllegalArgumentException(
                "description must contain 1 to 255 characters");
        }
        if (id != null && id < 0) {
            throw new IllegalArgumentException("id must be non-negative");
        }
        if (petId != null && petId < 0) {
            throw new IllegalArgumentException("petId must be non-negative");
        }
    }
}
