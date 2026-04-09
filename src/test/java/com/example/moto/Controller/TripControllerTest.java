package com.example.moto.Controller;

import com.example.moto.Trip;
import com.example.moto.User;
import com.example.moto.Services.TripService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TripController.class)
@TestPropertySource(properties = "spring.jackson.visibility.field=any")
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripService service;

    // =========================================================================
    // POST /api/users
    // =========================================================================

    @Nested
    @DisplayName("POST /api/users")
    class CreateUserEndpoint {

        @Test
        @DisplayName("devrait créer un utilisateur et retourner 200")
        void shouldCreateUser() throws Exception {
            // Arrange
            User user = new User("Lucas", false);
            ReflectionTestUtils.setField(user, "id", 1L);
            when(service.createUser("Lucas", false)).thenReturn(user);

            // Act & Assert
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Lucas\",\"premium\":false}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Lucas"))
                    .andExpect(jsonPath("$.premium").value(false))
                    .andExpect(jsonPath("$.points").value(0));

            verify(service).createUser("Lucas", false);
        }

        @Test
        @DisplayName("devrait créer un utilisateur premium et retourner 200")
        void shouldCreatePremiumUser() throws Exception {
            // Arrange
            User user = new User("VIPRider", true);
            ReflectionTestUtils.setField(user, "id", 2L);
            when(service.createUser("VIPRider", true)).thenReturn(user);

            // Act & Assert
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"VIPRider\",\"premium\":true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.name").value("VIPRider"))
                    .andExpect(jsonPath("$.premium").value(true));

            verify(service).createUser("VIPRider", true);
        }
    }

    // =========================================================================
    // POST /api/trips
    // =========================================================================

    @Nested
    @DisplayName("POST /api/trips")
    class CreateTripEndpoint {

        @Test
        @DisplayName("devrait créer un trajet et retourner 200")
        void shouldCreateTrip() throws Exception {
            // Arrange
            Trip trip = new Trip("Balade Alpine", 5, false);
            ReflectionTestUtils.setField(trip, "id", 1L);
            when(service.createTrip("Balade Alpine", 5, false)).thenReturn(trip);

            // Act & Assert
            mockMvc.perform(post("/api/trips")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Balade Alpine\",\"maxParticipants\":5,\"premiumOnly\":false}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Balade Alpine"))
                    .andExpect(jsonPath("$.maxParticipants").value(5))
                    .andExpect(jsonPath("$.premiumOnly").value(false))
                    .andExpect(jsonPath("$.started").value(false));

            verify(service).createTrip("Balade Alpine", 5, false);
        }

        @Test
        @DisplayName("devrait créer un trajet premium et retourner 200")
        void shouldCreatePremiumTrip() throws Exception {
            // Arrange
            Trip trip = new Trip("VIP Ride", 3, true);
            ReflectionTestUtils.setField(trip, "id", 2L);
            when(service.createTrip("VIP Ride", 3, true)).thenReturn(trip);

            // Act & Assert
            mockMvc.perform(post("/api/trips")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"VIP Ride\",\"maxParticipants\":3,\"premiumOnly\":true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.premiumOnly").value(true));

            verify(service).createTrip("VIP Ride", 3, true);
        }

        @Test
        @DisplayName("devrait lancer une exception si la capacité est invalide")
        void shouldThrowException_whenCapacityInvalid() throws Exception {
            // Arrange
            when(service.createTrip(anyString(), eq(0), anyBoolean()))
                    .thenThrow(new IllegalArgumentException("Invalid capacity"));

            // Act & Assert
            ServletException ex = assertThrows(ServletException.class, () ->
                    mockMvc.perform(post("/api/trips")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Bad\",\"maxParticipants\":0,\"premiumOnly\":false}"))
            );

            assertTrue(ex.getCause() instanceof IllegalArgumentException);
            assertEquals("Invalid capacity", ex.getCause().getMessage());
        }
    }

    // =========================================================================
    // POST /api/trips/{id}/join
    // =========================================================================

    @Nested
    @DisplayName("POST /api/trips/{id}/join")
    class JoinTripEndpoint {

        @Test
        @DisplayName("devrait rejoindre un trajet et retourner 200")
        void shouldJoinTrip() throws Exception {
            // Arrange
            Trip trip = new Trip("Balade", 5, false);
            ReflectionTestUtils.setField(trip, "id", 1L);
            when(service.joinTrip(1L, 1L)).thenReturn(trip);

            // Act & Assert
            mockMvc.perform(post("/api/trips/1/join")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Balade"));

            verify(service).joinTrip(1L, 1L);
        }

        @Test
        @DisplayName("devrait lancer une exception si le trajet n'existe pas")
        void shouldThrowException_whenTripNotFound() throws Exception {
            // Arrange
            when(service.joinTrip(eq(99L), anyLong()))
                    .thenThrow(new RuntimeException("Trip not found"));

            // Act & Assert
            ServletException ex = assertThrows(ServletException.class, () ->
                    mockMvc.perform(post("/api/trips/99/join")
                            .param("userId", "1"))
            );

            assertEquals("Trip not found", ex.getCause().getMessage());
        }

        @Test
        @DisplayName("devrait lancer une exception si l'utilisateur n'existe pas")
        void shouldThrowException_whenUserNotFound() throws Exception {
            // Arrange
            when(service.joinTrip(eq(1L), eq(99L)))
                    .thenThrow(new RuntimeException("User not found"));

            // Act & Assert
            ServletException ex = assertThrows(ServletException.class, () ->
                    mockMvc.perform(post("/api/trips/1/join")
                            .param("userId", "99"))
            );

            assertEquals("User not found", ex.getCause().getMessage());
        }

        @Test
        @DisplayName("devrait lancer une exception si le trajet est complet")
        void shouldThrowException_whenTripFull() throws Exception {
            // Arrange
            when(service.joinTrip(eq(1L), eq(2L)))
                    .thenThrow(new RuntimeException("Trip full"));

            // Act & Assert
            ServletException ex = assertThrows(ServletException.class, () ->
                    mockMvc.perform(post("/api/trips/1/join")
                            .param("userId", "2"))
            );

            assertEquals("Trip full", ex.getCause().getMessage());
        }

        @Test
        @DisplayName("devrait lancer une exception si non-premium sur trajet premium")
        void shouldThrowException_whenPremiumRequired() throws Exception {
            // Arrange
            when(service.joinTrip(eq(1L), eq(1L)))
                    .thenThrow(new RuntimeException("Premium required"));

            // Act & Assert
            ServletException ex = assertThrows(ServletException.class, () ->
                    mockMvc.perform(post("/api/trips/1/join")
                            .param("userId", "1"))
            );

            assertEquals("Premium required", ex.getCause().getMessage());
        }

        @Test
        @DisplayName("devrait lancer une exception si le trajet a déjà démarré")
        void shouldThrowException_whenTripAlreadyStarted() throws Exception {
            // Arrange
            when(service.joinTrip(eq(1L), eq(2L)))
                    .thenThrow(new RuntimeException("Trip already started"));

            // Act & Assert
            ServletException ex = assertThrows(ServletException.class, () ->
                    mockMvc.perform(post("/api/trips/1/join")
                            .param("userId", "2"))
            );

            assertEquals("Trip already started", ex.getCause().getMessage());
        }
    }

    // =========================================================================
    // POST /api/trips/{id}/start
    // =========================================================================

    @Nested
    @DisplayName("POST /api/trips/{id}/start")
    class StartTripEndpoint {

        @Test
        @DisplayName("devrait démarrer un trajet et retourner 200")
        void shouldStartTrip() throws Exception {
            // Arrange
            Trip trip = new Trip("Ready Ride", 5, false);
            ReflectionTestUtils.setField(trip, "id", 1L);
            ReflectionTestUtils.setField(trip, "started", true);
            when(service.startTrip(1L)).thenReturn(trip);

            // Act & Assert
            mockMvc.perform(post("/api/trips/1/start"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.started").value(true));

            verify(service).startTrip(1L);
        }

        @Test
        @DisplayName("devrait lancer une exception si aucun participant")
        void shouldThrowException_whenNoParticipants() throws Exception {
            // Arrange
            when(service.startTrip(1L))
                    .thenThrow(new RuntimeException("No participants"));

            // Act & Assert
            ServletException ex = assertThrows(ServletException.class, () ->
                    mockMvc.perform(post("/api/trips/1/start"))
            );

            assertEquals("No participants", ex.getCause().getMessage());
        }

        @Test
        @DisplayName("devrait lancer une exception si le trajet n'existe pas")
        void shouldThrowException_whenTripNotFound() throws Exception {
            // Arrange
            when(service.startTrip(99L))
                    .thenThrow(new RuntimeException("Trip not found"));

            // Act & Assert
            ServletException ex = assertThrows(ServletException.class, () ->
                    mockMvc.perform(post("/api/trips/99/start"))
            );

            assertEquals("Trip not found", ex.getCause().getMessage());
        }
    }

    // =========================================================================
    // GET /api/trips
    // =========================================================================

    @Nested
    @DisplayName("GET /api/trips")
    class GetTripsEndpoint {

        @Test
        @DisplayName("devrait retourner la liste des trajets")
        void shouldReturnAllTrips() throws Exception {
            // Arrange
            Trip trip1 = new Trip("Balade 1", 5, false);
            ReflectionTestUtils.setField(trip1, "id", 1L);
            Trip trip2 = new Trip("Balade 2", 3, true);
            ReflectionTestUtils.setField(trip2, "id", 2L);

            when(service.allTrips()).thenReturn(Arrays.asList(trip1, trip2));

            // Act & Assert
            mockMvc.perform(get("/api/trips"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Balade 1"))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].name").value("Balade 2"))
                    .andExpect(jsonPath("$[1].premiumOnly").value(true));

            verify(service).allTrips();
        }

        @Test
        @DisplayName("devrait retourner une liste vide s'il n'y a aucun trajet")
        void shouldReturnEmptyList_whenNoTrips() throws Exception {
            // Arrange
            when(service.allTrips()).thenReturn(Collections.emptyList());

            // Act & Assert
            mockMvc.perform(get("/api/trips"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(service).allTrips();
        }
    }
}
