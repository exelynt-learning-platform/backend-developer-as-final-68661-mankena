package com.example.resourcebooking;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.resourcebooking.entity.AppUser;
import com.example.resourcebooking.entity.Resource;
import com.example.resourcebooking.entity.Role;
import com.example.resourcebooking.repository.ResourceRepository;
import com.example.resourcebooking.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long meetingRoomId;

    @BeforeEach
    void setUp() {
        // DataSeeder already creates admin/admin123 and user/user123.
        // Add a second regular user for ownership-isolation tests.
        if (userRepository.findByUsername("user2").isEmpty()) {
            AppUser user2 = new AppUser();
            user2.setUsername("user2");
            user2.setPassword(passwordEncoder.encode("user2pass"));
            user2.setRole(Role.USER);
            userRepository.save(user2);
        }

        Resource resource = new Resource();
        resource.setName("Conference Room A");
        resource.setDescription("Large meeting room");
        resource.setType("ROOM");
        resource.setPrice(new java.math.BigDecimal("20.00")); // per hour
        resource.setAvailable(true);
        meetingRoomId = resourceRepository.save(resource).getId();
    }

    private String loginAs(String username, String password) throws Exception {

        String body = objectMapper.writeValueAsString(
                new java.util.HashMap<>() {{
                    put("username", username);
                    put("password", password);
                }});

        String response = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void userCannotCreateResource() throws Exception {

        String userToken = loginAs("user", "user123");

        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content("{\"name\":\"Hack\",\"type\":\"ROOM\",\"price\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanCreateReservationAndPriceIsServerComputed() throws Exception {

        String userToken = loginAs("user", "user123");

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        String requestBody = String.format(
                "{\"resourceId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                meetingRoomId, iso(start), iso(end));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                // 2 hours * $20/hr = $40, even though client never sent a price
                .andExpect(jsonPath("$.price").value(40.0));
    }

    @Test
    void overlappingReservationIsRejectedWithConflict() throws Exception {

        String userToken = loginAs("user", "user123");

        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = start.plusHours(2);

        String firstRequest = String.format(
                "{\"resourceId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                meetingRoomId, iso(start), iso(end));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(firstRequest))
                .andExpect(status().isCreated());

        // Overlapping window (starts 1 hour into the first booking)
        LocalDateTime overlapStart = start.plusHours(1);
        LocalDateTime overlapEnd = overlapStart.plusHours(2);

        String secondRequest = String.format(
                "{\"resourceId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                meetingRoomId, iso(overlapStart), iso(overlapEnd));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(secondRequest))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidReservationRequestReturns400WithFieldErrors() throws Exception {

        String userToken = loginAs("user", "user123");

        // Missing startTime/endTime entirely
        String badRequest = String.format("{\"resourceId\":%d}", meetingRoomId);

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(badRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.startTime").exists())
                .andExpect(jsonPath("$.fieldErrors.endTime").exists());
    }

    @Test
    void userCannotViewAnotherUsersReservation() throws Exception {

        String userToken = loginAs("user", "user123");
        String user2Token = loginAs("user2", "user2pass");

        LocalDateTime start = LocalDateTime.now().plusDays(3);
        LocalDateTime end = start.plusHours(1);

        String requestBody = String.format(
                "{\"resourceId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                meetingRoomId, iso(start), iso(end));

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long reservationId = objectMapper.readTree(response).get("id").asLong();

        // user2 tries to read user's reservation directly by id
        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());

        // user2 tries to delete it too
        mockMvc.perform(delete("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());

        // owner can still read their own
        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanViewAnyReservationAndSeesNoPasswordField() throws Exception {

        String adminToken = loginAs("admin", "admin123");
        String userToken = loginAs("user", "user123");

        LocalDateTime start = LocalDateTime.now().plusDays(4);
        LocalDateTime end = start.plusHours(1);

        String requestBody = String.format(
                "{\"resourceId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                meetingRoomId, iso(start), iso(end));

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long reservationId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("user"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void unknownReservationReturns404() throws Exception {

        String userToken = loginAs("user", "user123");

        mockMvc.perform(get("/reservations/999999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("999999")));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {

        String body = "{\"username\":\"admin\",\"password\":\"wrong-password\"}";

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidOrMissingTokenIsRejected() throws Exception {

        mockMvc.perform(get("/reservations"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminHasFullResourceCrudAndUserIsReadOnly() throws Exception {

        String adminToken = loginAs("admin", "admin123");
        String userToken = loginAs("user", "user123");

        String createBody = "{\"name\":\"Projector\",\"type\":\"EQUIPMENT\",\"price\":5.00,\"available\":true}";

        String response = mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Projector"))
                .andReturn().getResponse().getContentAsString();

        long resourceId = objectMapper.readTree(response).get("id").asLong();

        // USER can read
        mockMvc.perform(get("/resources/" + resourceId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // USER cannot update or delete
        mockMvc.perform(put("/resources/" + resourceId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content("{\"name\":\"Hacked\",\"type\":\"EQUIPMENT\",\"price\":1}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/resources/" + resourceId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // ADMIN can update and delete
        mockMvc.perform(put("/resources/" + resourceId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"name\":\"Projector v2\",\"type\":\"EQUIPMENT\",\"price\":6.00,\"available\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Projector v2"));

        mockMvc.perform(delete("/resources/" + resourceId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidResourceCreationReturns400() throws Exception {

        String adminToken = loginAs("admin", "admin123");

        // missing required "name" and "type"
        mockMvc.perform(post("/resources")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"price\":10.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.type").exists());
    }

    @Test
    void invalidStatusValueInBodyReturns400NotServerError() throws Exception {

        String userToken = loginAs("user", "user123");

        LocalDateTime start = LocalDateTime.now().plusDays(5);
        LocalDateTime end = start.plusHours(1);

        String requestBody = String.format(
                "{\"resourceId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                meetingRoomId, iso(start), iso(end));

        String response = mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long reservationId = objectMapper.readTree(response).get("id").asLong();

        String adminToken = loginAs("admin", "admin123");

        String updateWithBadStatus = String.format(
                "{\"resourceId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\",\"status\":\"NOT_A_REAL_STATUS\"}",
                meetingRoomId, iso(start), iso(end));

        mockMvc.perform(put("/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content(updateWithBadStatus))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filteringByStatusAndPriceRangeTogetherWorks() throws Exception {

        String adminToken = loginAs("admin", "admin123");
        String userToken = loginAs("user", "user123");

        LocalDateTime start = LocalDateTime.now().plusDays(6);

        // Reservation 1: 1 hour -> $20, stays PENDING
        String req1 = String.format(
                "{\"resourceId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                meetingRoomId, iso(start), iso(start.plusHours(1)));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(req1))
                .andExpect(status().isCreated());

        // Reservation 2: 5 hours -> $100, different day so it doesn't overlap
        LocalDateTime start2 = start.plusDays(1);
        String req2 = String.format(
                "{\"resourceId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                meetingRoomId, iso(start2), iso(start2.plusHours(5)));

        mockMvc.perform(post("/reservations")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType("application/json")
                        .content(req2))
                .andExpect(status().isCreated());

        // Filtering PENDING + price between 10 and 50 should return only the
        // $20 reservation, not the $100 one - proves status AND price both apply.
        mockMvc.perform(get("/reservations")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDING")
                        .param("minPrice", "10")
                        .param("maxPrice", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.price == 100.0)]").doesNotExist());
    }

    private String iso(LocalDateTime time) {
        return time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
