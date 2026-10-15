/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.rest.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.samples.petclinic.mapper.VisitMapper;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.rest.advice.ExceptionControllerAdvice;
import org.springframework.samples.petclinic.rest.controller.v1.VisitRestControllerV1;
import org.springframework.samples.petclinic.rest.dto.VisitDto;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.samples.petclinic.service.VisitBookingValidator;
import org.springframework.samples.petclinic.service.clinicService.ApplicationTestConfig;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Test class for {@link VisitRestControllerV1}.
 */
@SpringBootTest
@ContextConfiguration(classes = ApplicationTestConfig.class)
@WebAppConfiguration
@WithMockUser(roles = "OWNER_ADMIN")
class VisitRestControllerV1Tests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2030-06-15T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.now(CLOCK);

    @MockitoBean
    private ClinicService clinicService;

    @Autowired
    private VisitMapper visitMapper;

    private final ObjectMapper mapper = new ObjectMapper();
    private MockMvc mockMvc;
    private Pet pet;
    private List<Visit> visits;
    private List<Visit> savedVisits;
    private List<Visit> deletedVisits;

    @BeforeEach
    void initVisits() {
        VisitBookingValidator validator = new VisitBookingValidator(clinicService, CLOCK);
        this.mockMvc = MockMvcBuilders.standaloneSetup(
                new VisitRestControllerV1(clinicService, visitMapper, validator))
            .setControllerAdvice(new ExceptionControllerAdvice())
            .build();
        pet = new Pet();
        pet.setId(8);
        pet.setName("Rosy");
        visits = new ArrayList<>(List.of(
            visit(2, TODAY.plusDays(1), "rabies shot"),
            visit(3, TODAY.plusDays(2), "neutered")));
        savedVisits = new ArrayList<>();
        deletedVisits = new ArrayList<>();
        doAnswer(invocation -> {
            Visit saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(999);
            }
            savedVisits.add(saved);
            return null;
        }).when(clinicService).saveVisit(any(Visit.class));
        doAnswer(invocation -> {
            deletedVisits.add(invocation.getArgument(0));
            return null;
        }).when(clinicService).deleteVisit(any(Visit.class));
    }

    private Visit visit(Integer id, LocalDate date, String description) {
        Visit visit = new Visit();
        visit.setId(id);
        visit.setPet(pet);
        visit.setDate(date);
        visit.setDescription(description);
        return visit;
    }

    private void stubExistingPet() {
        when(clinicService.findPetById(anyInt())).thenReturn(pet);
        when(clinicService.findVisitsByPetId(8)).thenReturn(List.of());
    }

    private MockHttpServletResponse perform(String method, String path, String body) throws Exception {
        var builder = request(HttpMethod.valueOf(method), path).accept(MediaType.APPLICATION_JSON);
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(builder).andReturn().getResponse();
    }

    private String body(LocalDate date, String description, Integer petId) {
        return mapper.writeValueAsString(new VisitDto(date, description, null, petId));
    }

    private void assertBadRequest(MockHttpServletResponse response, String detail) throws Exception {
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(mapper.readTree(response.getContentAsString()).get("detail").asString()).isEqualTo(detail);
        assertThat(savedVisits).isEmpty();
        assertThat(deletedVisits).isEmpty();
    }

    @Test
    void shouldReturnVisitWhenVisitExists() throws Exception {
        when(clinicService.findVisitById(2)).thenReturn(visits.get(0));
        var response = perform("GET", "/api/visits/2", null);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(mapper.readValue(response.getContentAsString(), VisitDto.class))
            .isEqualTo(visitMapper.toVisitDto(visits.get(0)));
    }

    @ParameterizedTest
    @CsvSource({"GET, 999", "PUT, 999", "DELETE, 999"})
    void shouldReturnNotFoundWhenVisitDoesNotExist(String method, int id) throws Exception {
        when(clinicService.findVisitById(id)).thenReturn(null);
        var response = perform(method, "/api/visits/" + id,
            "PUT".equals(method) ? body(TODAY, "checkup", 8) : null);
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(savedVisits).isEmpty();
        assertThat(deletedVisits).isEmpty();
    }

    @Test
    void shouldReturnAllVisitsWhenVisitsExist() throws Exception {
        when(clinicService.findAllVisits()).thenReturn(visits);
        var response = perform("GET", "/api/visits", null);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(mapper.readValue(response.getContentAsString(), VisitDto[].class))
            .containsExactly(visitMapper.toVisitDto(visits.get(0)), visitMapper.toVisitDto(visits.get(1)));
    }

    @Test
    void shouldReturnNotFoundWhenNoVisitsExist() throws Exception {
        when(clinicService.findAllVisits()).thenReturn(List.of());
        var response = perform("GET", "/api/visits", null);
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentAsString()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"POST, 1", "POST, 30", "POST, 365", "PUT, 1", "PUT, 30"})
    void shouldReturnBadRequestWhenVisitDateIsInPast(String method, int daysInPast) throws Exception {
        when(clinicService.findVisitById(2)).thenReturn(visits.get(0));
        var response = perform(method, "POST".equals(method) ? "/api/visits" : "/api/visits/2",
            body(TODAY.minusDays(daysInPast), "checkup", 8));
        assertBadRequest(response, "Visit date must not be in the past");
        assertThat(visits.get(0).getDate()).isEqualTo(TODAY.plusDays(1));
        assertThat(visits.get(0).getDescription()).isEqualTo("rabies shot");
    }

    @ParameterizedTest
    @CsvSource({"POST, 999", "POST, 0", "PUT, 999"})
    void shouldReturnBadRequestWhenPetDoesNotExist(String method, int nonExistingId) throws Exception {
        when(clinicService.findPetById(nonExistingId)).thenReturn(null);
        pet.setId(nonExistingId);
        when(clinicService.findVisitById(2)).thenReturn(visits.get(0));
        var response = perform(method, "POST".equals(method) ? "/api/visits" : "/api/visits/2",
            body(TODAY, "checkup", nonExistingId));
        assertBadRequest(response, "Pet does not exist");
        assertThat(visits.get(0).getDate()).isEqualTo(TODAY.plusDays(1));
    }

    @ParameterizedTest
    @CsvSource({"POST, checkup", "POST, different description", "PUT, checkup"})
    void shouldReturnBadRequestWhenDuplicateVisitIsSubmitted(String method, String description) throws Exception {
        stubExistingPet();
        when(clinicService.findVisitById(2)).thenReturn(visits.get(0));
        when(clinicService.findVisitsByPetId(8)).thenReturn(visits);
        var response = perform(method, "POST".equals(method) ? "/api/visits" : "/api/visits/2",
            body(TODAY.plusDays(2), description, 8));
        assertBadRequest(response, "A visit is already booked for this pet and date");
        assertThat(visits.get(0).getDate()).isEqualTo(TODAY.plusDays(1));
        assertThat(visits.get(0).getDescription()).isEqualTo("rabies shot");
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "1, 255", "30, 10"})
    void shouldCreateVisitWhenPayloadIsValid(int daysAhead, int descriptionLength) throws Exception {
        stubExistingPet();
        when(clinicService.findVisitsByPetId(8)).thenReturn(List.of(visit(2, TODAY.minusDays(1), "previous")));
        LocalDate date = TODAY.plusDays(daysAhead);
        String description = "x".repeat(descriptionLength);
        var response = perform("POST", "/api/visits", body(date, description, 8));
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getHeader("Location")).isEqualTo("/api/visits/999");
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat(mapper.readValue(response.getContentAsString(), VisitDto.class))
            .isEqualTo(new VisitDto(date, description, 999, 8));
        assertThat(savedVisits).hasSize(1);
        assertThat(savedVisits.get(0).getPet()).isSameAs(pet);
        assertThat(savedVisits.get(0).getDate()).isEqualTo(date);
        assertThat(savedVisits.get(0).getDescription()).isEqualTo(description);
    }

    @Test
    void shouldAllowSameDateForDifferentPets() throws Exception {
        stubExistingPet();
        when(clinicService.findVisitsByPetId(9)).thenReturn(visits);
        pet.setId(9);
        Pet otherPet = new Pet();
        otherPet.setId(8);
        when(clinicService.findPetById(8)).thenReturn(otherPet);
        var response = perform("POST", "/api/visits", body(TODAY.plusDays(1), "checkup", 8));
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(savedVisits).hasSize(1);
        assertThat(savedVisits.get(0).getPet()).isSameAs(otherPet);
    }

    @ParameterizedTest
    @CsvSource({"POST, true", "POST, false", "PUT, true", "PUT, false"})
    void shouldAllowOptionalDateToBeMissingOrNull(String method, boolean includeDate) throws Exception {
        stubExistingPet();
        when(clinicService.findVisitById(2)).thenReturn(visits.get(0));
        var response = perform(method, "POST".equals(method) ? "/api/visits" : "/api/visits/2",
            includeDate ? body(null, "undated visit", 8) : "{\"description\":\"undated visit\",\"petId\":8}");
        assertThat(response.getStatus()).isEqualTo("POST".equals(method) ? 201 : 204);
        assertThat(savedVisits).hasSize(1);
        assertThat(savedVisits.get(0).getDate()).isNull();
    }

    @Test
    void shouldReturnBadRequestWhenDuplicateVisitIncludesExistingVisitId() throws Exception {
        stubExistingPet();
        when(clinicService.findVisitsByPetId(8)).thenReturn(visits);
        var response = perform("POST", "/api/visits", mapper.writeValueAsString(
            new VisitDto(TODAY.plusDays(1), "different description", 2, 8)));
        assertBadRequest(response, "A visit is already booked for this pet and date");
    }

    @ParameterizedTest
    @CsvSource({"0", "1", "30"})
    void shouldUpdateVisitWithoutConflictingWithItself(int daysAhead) throws Exception {
        stubExistingPet();
        when(clinicService.findVisitById(2)).thenReturn(visits.get(0));
        when(clinicService.findVisitsByPetId(8)).thenReturn(List.of(visits.get(0)));
        var response = perform("PUT", "/api/visits/2", body(TODAY.plusDays(daysAhead), "updated checkup", 8));
        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(savedVisits).containsExactly(visits.get(0));
        assertThat(visits.get(0).getDate()).isEqualTo(TODAY.plusDays(daysAhead));
        assertThat(visits.get(0).getDescription()).isEqualTo("updated checkup");
        var readBack = perform("GET", "/api/visits/2", null);
        assertThat(readBack.getStatus()).isEqualTo(200);
        assertThat(mapper.readValue(readBack.getContentAsString(), VisitDto.class))
            .isEqualTo(new VisitDto(TODAY.plusDays(daysAhead), "updated checkup", 2, 8));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "POST|{}",
        "POST|{\"date\":\"2030-06-15\",\"petId\":8}",
        "POST|{\"description\":null,\"petId\":8}",
        "POST|{\"description\":\"\",\"petId\":8}",
        "POST|{\"description\":\"checkup\"}",
        "POST|{\"description\":\"checkup\",\"petId\":null}",
        "POST|{\"description\":\"checkup\",\"petId\":-1}",
        "POST|{\"description\":\"checkup\",\"petId\":8,\"id\":-1}",
        "POST|{\"description\":\"checkup\",\"petId\":8,\"date\":\"2030-02-30\"}",
        "POST|{\"description\":\"checkup\",\"petId\":\"not-a-number\"}",
        "POST|{broken",
        "POST|null",
        "POST|[]",
        "POST|''",
        "PUT|{}",
        "PUT|{\"description\":null}",
        "PUT|{\"description\":\"\"}",
        "PUT|{\"description\":\"checkup\",\"date\":\"invalid\"}",
        "PUT|{broken",
        "PUT|''"
    })
    void shouldReturnBadRequestWhenRequestPayloadIsInvalid(String method, String payload) throws Exception {
        var response = perform(method, "POST".equals(method) ? "/api/visits" : "/api/visits/2", payload);
        assertBadRequest(response, "The request contains invalid or missing parameters");
        assertThat(mockingDetails(clinicService).getInvocations()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"POST, 256", "POST, 1000", "PUT, 256", "PUT, 1000"})
    void shouldReturnBadRequestWhenDescriptionExceedsMaximumLength(String method, int length) throws Exception {
        String payload = "{\"date\":\"2030-06-15\",\"description\":\"" + "x".repeat(length) + "\",\"petId\":8}";
        var response = perform(method, "POST".equals(method) ? "/api/visits" : "/api/visits/2", payload);
        assertBadRequest(response, "The request contains invalid or missing parameters");
        assertThat(mockingDetails(clinicService).getInvocations()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "POST|{\"description\":null,\"petId\":8}|description",
        "POST|{\"description\":\"checkup\",\"petId\":null}|petId",
        "PUT|{\"description\":null}|description",
        "PUT|{\"description\":\"\"}|description"
    })
    void shouldReportFieldValidationErrorsForInvalidPayload(String method, String payload, String field) throws Exception {
        var response = perform(method, "POST".equals(method) ? "/api/visits" : "/api/visits/2", payload);
        assertBadRequest(response, "The request contains invalid or missing parameters");
        var problem = mapper.readTree(response.getContentAsString());
        assertThat(problem.get("title").asString()).isEqualTo("MethodArgumentNotValidException");
        assertThat(problem.get("schemaValidationErrors").size()).isEqualTo(1);
        var error = problem.get("schemaValidationErrors").get(0);
        assertThat(error.get("field").asString()).isEqualTo(field);
        assertThat(error.get("message").asString()).isNotBlank();
        assertThat(error.get("defaultMessage").asString()).isNotBlank();
        assertThat(mockingDetails(clinicService).getInvocations()).isEmpty();
    }

    @Test
    void shouldDeleteVisitWhenVisitExists() throws Exception {
        when(clinicService.findVisitById(2)).thenReturn(visits.get(0));
        var response = perform("DELETE", "/api/visits/2", null);
        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(response.getContentAsString()).isEmpty();
        assertThat(deletedVisits).containsExactly(visits.get(0));
        assertThat(savedVisits).isEmpty();
    }

    @Test
    void shouldReturnServerErrorWhenSavingVisitFails() throws Exception {
        stubExistingPet();
        doThrow(new IllegalStateException("internal failure")).when(clinicService).saveVisit(any(Visit.class));
        var response = perform("POST", "/api/visits", body(TODAY, "checkup", 8));
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(mapper.readTree(response.getContentAsString()).get("detail").asString())
            .isEqualTo("An unexpected error occurred while processing your request");
        assertThat(response.getContentAsString()).doesNotContain("internal failure");
    }
}
