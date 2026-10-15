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

import java.text.SimpleDateFormat;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.mapper.OwnerMapper;
import org.springframework.samples.petclinic.mapper.PetMapper;
import org.springframework.samples.petclinic.mapper.VisitMapper;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.rest.advice.ExceptionControllerAdvice;
import org.springframework.samples.petclinic.rest.controller.v1.OwnerRestControllerV1;
import org.springframework.samples.petclinic.rest.dto.OwnerDto;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.rest.dto.PetTypeDto;
import org.springframework.samples.petclinic.rest.dto.VisitDto;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.samples.petclinic.service.VisitBookingValidator;
import org.springframework.samples.petclinic.service.clinicService.ApplicationTestConfig;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;


/**
 * Test class for {@link OwnerRestControllerV1}
 *
 * @author Vitaliy Fedoriv
 */
@SpringBootTest
@ContextConfiguration(classes = ApplicationTestConfig.class)
@WebAppConfiguration
class OwnerRestControllerV1Tests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2030-06-15T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.now(CLOCK);
    private static final LocalDate BIRTH_DATE = LocalDate.of(2020, 1, 15);

    @Autowired
    private OwnerRestControllerV1 ownerRestControllerV1;

    @Autowired
    private OwnerMapper ownerMapper;

    @Autowired
    private PetMapper petMapper;

    @Autowired
    private VisitMapper visitMapper;

    @MockitoBean
    private ClinicService clinicService;

    private MockMvc mockMvc;

    private List<OwnerDto> owners;

    private List<PetDto> pets;

    @BeforeEach
    void initOwners() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(ownerRestControllerV1)
            .setControllerAdvice(new ExceptionControllerAdvice())
            .build();
        owners = new ArrayList<>();

        OwnerDto ownerWithPet = new OwnerDto();
        owners.add(ownerWithPet.id(1).firstName("George").lastName("Franklin").address("110 W. Liberty St.").city("Madison").telephone("6085551023").addPetsItem(getTestPetWithIdAndName(1, "Rosy")));
        OwnerDto owner = new OwnerDto();
        owners.add(owner.id(2).firstName("Betty").lastName("Davis").address("638 Cardinal Ave.").city("Sun Prairie").telephone("6085551749"));
        owner = new OwnerDto();
        owners.add(owner.id(3).firstName("Eduardo").lastName("Rodriquez").address("2693 Commerce St.").city("McFarland").telephone("6085558763"));
        owner = new OwnerDto();
        owners.add(owner.id(4).firstName("Harold").lastName("Davis").address("563 Friendly St.").city("Windsor").telephone("6085553198"));

        PetTypeDto petType = new PetTypeDto();
        petType.id(2)
            .name("dog");

        pets = new ArrayList<>();
        PetDto pet = new PetDto("Rosy", BIRTH_DATE, petType, 3, null, List.of());
        pets.add(pet);
        pet = new PetDto("Jewel", BIRTH_DATE, petType, 4, null, List.of());
        pets.add(pet);

    }

    private PetDto getTestPetWithIdAndName(final int id, final String name) {
        PetTypeDto petType = new PetTypeDto();
        petType.id(2).name("dog");
        return new PetDto(name, BIRTH_DATE, petType, id, null, List.of(getTestVisitForPet(1)));
    }

    private VisitDto getTestVisitForPet(final int id) {
        return new VisitDto(TODAY, "test" + id, id, 1);
    }

    private PetDto withId(PetDto pet, Integer id) {
        return new PetDto(pet.name(), pet.birthDate(), pet.type(), id, pet.ownerId(), pet.visits());
    }

    private PetDto withName(PetDto pet, String name) {
        return new PetDto(name, pet.birthDate(), pet.type(), pet.id(), pet.ownerId(), pet.visits());
    }

    private PetDto withBirthDate(PetDto pet, LocalDate birthDate) {
        return new PetDto(pet.name(), birthDate, pet.type(), pet.id(), pet.ownerId(), pet.visits());
    }

    private PetDto withType(PetDto pet, PetTypeDto type) {
        return new PetDto(pet.name(), pet.birthDate(), type, pet.id(), pet.ownerId(), pet.visits());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetOwnerSuccess() throws Exception {
        given(this.clinicService.findOwnerById(1)).willReturn(ownerMapper.toOwner(owners.get(0)));
        this.mockMvc.perform(get("/api/owners/1")
                .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200))
            .andExpect(result -> assertThat(result.getResponse().getContentType()).isEqualTo("application/json"))
            .andExpect(result -> assertThat(new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("id").intValue()).isEqualTo(1))
            .andExpect(result -> assertThat(new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("firstName").asString()).isEqualTo("George"));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetOwnerNotFound() throws Exception {
        given(this.clinicService.findOwnerById(2)).willReturn(null);
        this.mockMvc.perform(get("/api/owners/2")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(404));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetOwnersListSuccess() throws Exception {
        owners.remove(0);
        owners.remove(1);
        given(this.clinicService.findOwnerByLastName("Davis")).willReturn(ownerMapper.toOwners(owners));
        this.mockMvc.perform(get("/api/owners?lastName=Davis")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200))
            .andExpect(result -> assertThat(result.getResponse().getContentType()).isEqualTo("application/json"))
            .andExpect(result -> assertThat(new ObjectMapper().readValue(
                result.getResponse().getContentAsString(), OwnerDto[].class))
                .extracting(OwnerDto::getId).containsExactly(2, 4))
            .andExpect(result -> assertThat(new ObjectMapper().readValue(
                result.getResponse().getContentAsString(), OwnerDto[].class))
                .extracting(OwnerDto::getFirstName).containsExactly("Betty", "Harold"));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetOwnersListNotFound() throws Exception {
        owners.clear();
        given(this.clinicService.findOwnerByLastName("0")).willReturn(ownerMapper.toOwners(owners));
        this.mockMvc.perform(get("/api/owners?lastName=0")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(404));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetAllOwnersSuccess() throws Exception {
        owners.remove(0);
        owners.remove(1);
        given(this.clinicService.findAllOwners()).willReturn(ownerMapper.toOwners(owners));
        this.mockMvc.perform(get("/api/owners")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200))
            .andExpect(result -> assertThat(result.getResponse().getContentType()).isEqualTo("application/json"))
            .andExpect(result -> assertThat(new ObjectMapper().readValue(
                result.getResponse().getContentAsString(), OwnerDto[].class))
                .extracting(OwnerDto::getId).containsExactly(2, 4))
            .andExpect(result -> assertThat(new ObjectMapper().readValue(
                result.getResponse().getContentAsString(), OwnerDto[].class))
                .extracting(OwnerDto::getFirstName).containsExactly("Betty", "Harold"));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetAllOwnersNotFound() throws Exception {
        owners.clear();
        given(this.clinicService.findAllOwners()).willReturn(ownerMapper.toOwners(owners));
        this.mockMvc.perform(get("/api/owners")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(404));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreateOwnerSuccess() throws Exception {
        OwnerDto newOwnerDto = owners.get(0);
        newOwnerDto.setId(null);
        ObjectMapper mapper = new ObjectMapper();
        String newOwnerAsJSON = mapper.writeValueAsString(newOwnerDto);
        this.mockMvc.perform(post("/api/owners")
                .content(newOwnerAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(201));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreateOwnerError() throws Exception {
        OwnerDto newOwnerDto = owners.get(0);
        newOwnerDto.setId(null);
        newOwnerDto.setFirstName(null);
        ObjectMapper mapper = new ObjectMapper();
        String newOwnerAsJSON = mapper.writeValueAsString(newOwnerDto);
        this.mockMvc.perform(post("/api/owners")
                .content(newOwnerAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(400));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdateOwnerSuccess() throws Exception {
        given(this.clinicService.findOwnerById(1)).willReturn(ownerMapper.toOwner(owners.get(0)));
        int ownerId = owners.get(0).getId();
        OwnerDto updatedOwnerDto = new OwnerDto();
        // body.id = ownerId which is used in url path
        updatedOwnerDto.setId(ownerId);
        updatedOwnerDto.setFirstName("GeorgeI");
        updatedOwnerDto.setLastName("Franklin");
        updatedOwnerDto.setAddress("110 W. Liberty St.");
        updatedOwnerDto.setCity("Madison");
        updatedOwnerDto.setTelephone("6085551023");
        ObjectMapper mapper = new ObjectMapper();
        String newOwnerAsJSON = mapper.writeValueAsString(updatedOwnerDto);
        this.mockMvc.perform(put("/api/owners/" + ownerId)
                .content(newOwnerAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(result -> assertThat(result.getResponse().getContentType()).isEqualTo("application/json"))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(204));

        this.mockMvc.perform(get("/api/owners/" + ownerId)
                .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200))
            .andExpect(result -> assertThat(result.getResponse().getContentType()).isEqualTo("application/json"))
            .andExpect(result -> assertThat(new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("id").intValue()).isEqualTo(ownerId))
            .andExpect(result -> assertThat(new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("firstName").asString()).isEqualTo("GeorgeI"));

    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdateOwnerSuccessNoBodyId() throws Exception {
        given(this.clinicService.findOwnerById(1)).willReturn(ownerMapper.toOwner(owners.get(0)));
        int ownerId = owners.get(0).getId();
        OwnerDto updatedOwnerDto = new OwnerDto();
        updatedOwnerDto.setFirstName("GeorgeI");
        updatedOwnerDto.setLastName("Franklin");
        updatedOwnerDto.setAddress("110 W. Liberty St.");
        updatedOwnerDto.setCity("Madison");

        updatedOwnerDto.setTelephone("6085551023");
        ObjectMapper mapper = new ObjectMapper();
        String newOwnerAsJSON = mapper.writeValueAsString(updatedOwnerDto);
        this.mockMvc.perform(put("/api/owners/" + ownerId)
                .content(newOwnerAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(result -> assertThat(result.getResponse().getContentType()).isEqualTo("application/json"))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(204));

        this.mockMvc.perform(get("/api/owners/" + ownerId)
                .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200))
            .andExpect(result -> assertThat(result.getResponse().getContentType()).isEqualTo("application/json"))
            .andExpect(result -> assertThat(new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("id").intValue()).isEqualTo(ownerId))
            .andExpect(result -> assertThat(new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("firstName").asString()).isEqualTo("GeorgeI"));

    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdateOwnerError() throws Exception {
        OwnerDto newOwnerDto = owners.get(0);
        newOwnerDto.setFirstName("");
        ObjectMapper mapper = new ObjectMapper();
        String newOwnerAsJSON = mapper.writeValueAsString(newOwnerDto);
        this.mockMvc.perform(put("/api/owners/1")
                .content(newOwnerAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(400));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testDeleteOwnerSuccess() throws Exception {
        OwnerDto newOwnerDto = owners.get(0);
        ObjectMapper mapper = new ObjectMapper();
        String newOwnerAsJSON = mapper.writeValueAsString(newOwnerDto);
        final Owner owner = ownerMapper.toOwner(owners.get(0));
        given(this.clinicService.findOwnerById(1)).willReturn(owner);
        this.mockMvc.perform(delete("/api/owners/1")
                .content(newOwnerAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(204));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testDeleteOwnerError() throws Exception {
        OwnerDto newOwnerDto = owners.get(0);
        ObjectMapper mapper = new ObjectMapper();
        String newOwnerAsJSON = mapper.writeValueAsString(newOwnerDto);
        given(this.clinicService.findOwnerById(999)).willReturn(null);
        this.mockMvc.perform(delete("/api/owners/999")
                .content(newOwnerAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(404));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetSuccess() throws Exception {
        final Owner owner = ownerMapper.toOwner(owners.get(0));
        given(this.clinicService.findOwnerById(1)).willReturn(owner);
        PetDto newPet = withId(pets.get(0), 999);
        ObjectMapper mapper =  JsonMapper.builder()
            .defaultDateFormat(new SimpleDateFormat("dd/MM/yyyy"))
            .build();
        String newPetAsJSON = mapper.writeValueAsString(newPet);
        System.err.println("--> newPetAsJSON=" + newPetAsJSON);
        this.mockMvc.perform(post("/api/owners/1/pets")
                .content(newPetAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(201));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetError() throws Exception {
        PetDto newPet = pets.get(0);
        newPet = withName(withId(newPet, null), null);
        ObjectMapper mapper =  JsonMapper.builder()
            .defaultDateFormat(new SimpleDateFormat("dd/MM/yyyy"))
            .build();
        String newPetAsJSON = mapper.writeValueAsString(newPet);
        this.mockMvc.perform(post("/api/owners/1/pets")
                .content(newPetAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(400))
            .andDo(MockMvcResultHandlers.print());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetShouldNotExposeTechnicalDetails() throws Exception {
        PetDto newPet = pets.get(0);
        newPet = withId(newPet, null);
        ObjectMapper mapper =  JsonMapper.builder()
            .defaultDateFormat(new SimpleDateFormat("dd/MM/yyyy"))
            .build();
        String newPetAsJSON = mapper.writeValueAsString(newPet);
        String technicalMessage = "could not execute statement; SQL [insert into pets ...]; constraint [fk_pet_owner]";
        given(this.clinicService.findOwnerById(1)).willReturn(ownerMapper.toOwner(owners.get(0)));
        doThrow(new DataIntegrityViolationException(technicalMessage)).when(this.clinicService).savePet(any());
        this.mockMvc.perform(post("/api/owners/1/pets")
                .content(newPetAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(404))
            .andExpect(result -> assertThat(new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("detail").asString())
                .isEqualTo("The requested resource could not be processed due to a data constraint violation"));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetWithUnknownOwnerShouldReturnNotFound() throws Exception {
        PetDto newPet = pets.get(0);
        newPet = withId(newPet, null);
        ObjectMapper mapper = JsonMapper.builder()
            .defaultDateFormat(new SimpleDateFormat("dd/MM/yyyy"))
            .build();
        String newPetAsJSON = mapper.writeValueAsString(newPet);
        given(this.clinicService.findOwnerById(1000000)).willReturn(null);
        this.mockMvc.perform(post("/api/owners/1000000/pets")
                .content(newPetAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(404));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetWithNullTypeShouldReturnBadRequestWithGenericDetail() throws Exception {
        PetDto newPet = pets.get(0);
        newPet = withType(withId(newPet, null), null);
        ObjectMapper mapper = JsonMapper.builder()
            .defaultDateFormat(new SimpleDateFormat("dd/MM/yyyy"))
            .build();
        String newPetAsJSON = mapper.writeValueAsString(newPet);
        this.mockMvc.perform(post("/api/owners/1/pets")
                .content(newPetAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(this::assertValidationFailure);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetWithEmptyTypeNameShouldReturnBadRequestWithGenericDetail() throws Exception {
        PetDto newPet = pets.get(0);
        newPet = withId(newPet, null);
        newPet.type().setName("");
        ObjectMapper mapper = JsonMapper.builder()
            .defaultDateFormat(new SimpleDateFormat("dd/MM/yyyy"))
            .build();
        String newPetAsJSON = mapper.writeValueAsString(newPet);
        this.mockMvc.perform(post("/api/owners/1/pets")
                .content(newPetAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(this::assertValidationFailure);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetWithNullTypeIdShouldReturnBadRequestWithGenericDetail() throws Exception {
        PetDto newPet = pets.get(0);
        newPet = withId(newPet, null);
        newPet.type().setId(null);
        ObjectMapper mapper = JsonMapper.builder()
            .defaultDateFormat(new SimpleDateFormat("dd/MM/yyyy"))
            .build();
        String newPetAsJSON = mapper.writeValueAsString(newPet);
        this.mockMvc.perform(post("/api/owners/1/pets")
                .content(newPetAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(this::assertValidationFailure);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreatePetWithUnexpectedErrorShouldReturnInternalServerErrorWithGenericDetail() throws Exception {
        PetDto newPet = pets.get(0);
        newPet = withId(newPet, null);
        ObjectMapper mapper = JsonMapper.builder()
            .defaultDateFormat(new SimpleDateFormat("dd/MM/yyyy"))
            .build();
        String newPetAsJSON = mapper.writeValueAsString(newPet);
        given(this.clinicService.findOwnerById(1)).willReturn(ownerMapper.toOwner(owners.get(0)));
        doThrow(new IllegalStateException("JDBC timeout while executing insert into pets"))
            .when(this.clinicService).savePet(any());
        this.mockMvc.perform(post("/api/owners/1/pets")
                .content(newPetAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(500))
            .andExpect(result -> assertThat(new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("detail").asString()).isEqualTo("An unexpected error occurred while processing your request"))
            .andExpect(result -> assertThat(new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("title").asString()).isEqualTo("IllegalStateException"))
            .andExpect(result -> assertThat(new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("timestamp")).isNotNull());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreateOwnerWithUnexpectedErrorShouldReturnInternalServerErrorWithGenericDetail() throws Exception {
        OwnerDto newOwnerDto = owners.get(0);
        newOwnerDto.setId(null);
        ObjectMapper mapper = new ObjectMapper();
        String newOwnerAsJSON = mapper.writeValueAsString(newOwnerDto);
        doThrow(new RuntimeException("Low-level persistence exception details"))
            .when(this.clinicService).saveOwner(any());
        this.mockMvc.perform(post("/api/owners")
                .content(newOwnerAsJSON).accept(MediaType.APPLICATION_JSON_VALUE).contentType(MediaType.APPLICATION_JSON_VALUE))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(500))
            .andExpect(result -> assertThat(new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("detail").asString()).isEqualTo("An unexpected error occurred while processing your request"))
            .andExpect(result -> assertThat(new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("title").asString()).isEqualTo("RuntimeException"))
            .andExpect(result -> assertThat(new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .get("timestamp")).isNotNull());
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testCreateVisitSuccess() throws Exception {
        Pet pet = new Pet();
        pet.setId(1);
        when(clinicService.findPetById(anyInt())).thenReturn(pet);
        when(clinicService.findVisitsByPetId(1)).thenReturn(List.of());
        List<Visit> savedVisits = new ArrayList<>();
        doAnswer(invocation -> {
            Visit visit = invocation.getArgument(0);
            visit.setId(999);
            savedVisits.add(visit);
            return null;
        }).when(clinicService).saveVisit(any(Visit.class));
        var response = visitBookingMockMvc().perform(post("/api/owners/1/pets/1/visits")
                .content("{\"date\":\"2030-06-15\",\"description\":\"checkup\"}")
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
            .andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getHeader("Location")).isEqualTo("/api/visits/999");
        assertThat(new ObjectMapper().readValue(response.getContentAsString(), VisitDto.class))
            .isEqualTo(new VisitDto(LocalDate.of(2030, 6, 15), "checkup", 999, 1));
        assertThat(savedVisits).hasSize(1);
        assertThat(savedVisits.get(0).getPet()).isSameAs(pet);
    }

    private MockMvc visitBookingMockMvc() {
        return MockMvcBuilders.standaloneSetup(new OwnerRestControllerV1(clinicService,
                ownerMapper, petMapper, visitMapper, new VisitBookingValidator(clinicService, CLOCK)))
            .setControllerAdvice(new ExceptionControllerAdvice())
            .build();
    }

    @ParameterizedTest
    @CsvSource({"2030-06-14", "2030-05-15", "2029-06-15"})
    void shouldReturnBadRequestWhenOwnerVisitDateIsInPast(LocalDate date) throws Exception {
        assertOwnerVisitBadRequest(1, "{\"date\":\"" + date + "\",\"description\":\"checkup\"}",
            "Visit date must not be in the past");
    }

    @ParameterizedTest
    @CsvSource({"999", "0"})
    void shouldReturnBadRequestWhenOwnerVisitPetDoesNotExist(int nonExistingId) throws Exception {
        when(clinicService.findPetById(nonExistingId)).thenReturn(null);
        assertOwnerVisitBadRequest(nonExistingId, "{\"date\":\"2030-06-15\",\"description\":\"checkup\"}",
            "Pet does not exist");
    }

    @ParameterizedTest
    @CsvSource({"checkup", "different description"})
    void shouldReturnBadRequestWhenDuplicateOwnerVisitIsSubmitted(String description) throws Exception {
        Pet pet = new Pet();
        pet.setId(1);
        Visit existingVisit = new Visit();
        existingVisit.setId(2);
        existingVisit.setPet(pet);
        existingVisit.setDate(LocalDate.of(2030, 6, 15));
        existingVisit.setDescription("checkup");
        when(clinicService.findPetById(1)).thenReturn(pet);
        when(clinicService.findVisitsByPetId(1)).thenReturn(List.of(existingVisit));
        assertOwnerVisitBadRequest(1, "{\"date\":\"2030-06-15\",\"description\":\"" + description + "\"}",
            "A visit is already booked for this pet and date");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "{}",
        "{\"description\":null}",
        "{\"description\":\"\"}",
        "{\"date\":\"invalid\",\"description\":\"checkup\"}",
        "{broken"
    })
    void shouldReturnBadRequestWhenOwnerVisitPayloadIsInvalid(String payload) throws Exception {
        assertOwnerVisitBadRequest(1, payload, "The request contains invalid or missing parameters");
    }

    private void assertOwnerVisitBadRequest(int petId, String payload, String detail) throws Exception {
        List<Visit> savedVisits = new ArrayList<>();
        doAnswer(invocation -> {
            savedVisits.add(invocation.getArgument(0));
            return null;
        }).when(clinicService).saveVisit(any(Visit.class));
        var response = visitBookingMockMvc().perform(post("/api/owners/1/pets/" + petId + "/visits")
                .content(payload).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
            .andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(new ObjectMapper().readTree(response.getContentAsString()).get("detail").asString())
            .isEqualTo(detail);
        assertThat(savedVisits).isEmpty();
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetOwnerPetSuccess() throws Exception {
        var owner = ownerMapper.toOwner(owners.get(0));
        given(this.clinicService.findOwnerById(2)).willReturn(owner);
        var pet = petMapper.toPet(pets.get(0));
        pet.setOwner(owner);
        given(this.clinicService.findPetById(1)).willReturn(pet);
        this.mockMvc.perform(get("/api/owners/2/pets/1")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(200))
            .andExpect(result -> assertThat(result.getResponse().getContentType()).isEqualTo("application/json"));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetOwnersPetsWithOwnerNotFound() throws Exception {
        owners.clear();
        given(this.clinicService.findAllOwners()).willReturn(ownerMapper.toOwners(owners));
        this.mockMvc.perform(get("/api/owners/1/pets/1")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(404));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testGetOwnersPetsWithPetNotFound() throws Exception {
        var owner1 = ownerMapper.toOwner(owners.get(0));
        given(this.clinicService.findOwnerById(1)).willReturn(owner1);
        this.mockMvc.perform(get("/api/owners/1/pets/2")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(404));
    }


    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdateOwnersPetSuccess() throws Exception {
        int ownerId = owners.get(0).getId();
        int petId = pets.get(0).id();
        given(this.clinicService.findOwnerById(ownerId)).willReturn(ownerMapper.toOwner(owners.get(0)));
        given(this.clinicService.findPetById(petId)).willReturn(petMapper.toPet(pets.get(0)));
        PetDto updatedPetDto = withBirthDate(withName(pets.get(0), "Rex"), LocalDate.of(2020, 1, 15));
        ObjectMapper mapper =  JsonMapper.builder()
            .defaultDateFormat(new SimpleDateFormat("dd/MM/yyyy"))
            .build();
        String updatedPetAsJSON = mapper.writeValueAsString(updatedPetDto);
        this.mockMvc.perform(put("/api/owners/" + ownerId + "/pets/" + petId)
                .content(updatedPetAsJSON)
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(204));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdateOwnersPetOwnerNotFound() throws Exception {
        int ownerId = 0;
        int petId = pets.get(0).id();
        given(this.clinicService.findOwnerById(ownerId)).willReturn(null);
        PetDto petDto = withName(pets.get(0), "Thor");
        ObjectMapper mapper =  JsonMapper.builder()
            .defaultDateFormat(new SimpleDateFormat("dd/MM/yyyy"))
            .build();
        String updatedPetAsJSON = mapper.writeValueAsString(petDto);
        this.mockMvc.perform(put("/api/owners/" + ownerId + "/pets/" + petId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedPetAsJSON))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(404));
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void testUpdateOwnersPetPetNotFound() throws Exception {
        int ownerId = owners.get(0).getId();
        int petId = 0;
        given(this.clinicService.findOwnerById(ownerId)).willReturn(ownerMapper.toOwner(owners.get(0)));
        given(this.clinicService.findPetById(petId)).willReturn(null);
        PetDto petDto = withBirthDate(withName(pets.get(0), "Ghost"), LocalDate.of(2020, 1, 1));
        ObjectMapper mapper =  JsonMapper.builder()
            .defaultDateFormat(new SimpleDateFormat("dd/MM/yyyy"))
            .build();
        String updatedPetAsJSON = mapper.writeValueAsString(petDto);
        this.mockMvc.perform(put("/api/owners/" + ownerId + "/pets/" + petId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedPetAsJSON))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(404));
    }

    private void assertValidationFailure(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        var problem = new ObjectMapper().readTree(result.getResponse().getContentAsString());
        assertThat(problem.get("detail").asString()).isEqualTo("The request contains invalid or missing parameters");
        assertThat(problem.get("title").asString()).isEqualTo("MethodArgumentNotValidException");
        var errors = problem.get("schemaValidationErrors");
        assertThat(errors.isArray()).isTrue();
        assertThat(errors.size()).isGreaterThan(0);
        var error = errors.get(0);
        assertThat(error.get("message")).isNotNull();
        assertThat(error.get("field")).isNotNull();
        assertThat(error.get("rejectedValue")).isNotNull();
        assertThat(error.get("defaultMessage")).isNotNull();
    }

}
