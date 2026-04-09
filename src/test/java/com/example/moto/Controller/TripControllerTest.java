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
class TripControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private TripService service;

        @Nested
        @DisplayName("POST /api/users")
        class CreateUserEndpoint {

                @Test
                @DisplayName("devrait appeler le service pour créer un utilisateur et retourner 200")
                void shouldCreateUser() throws Exception {
                        User user = new User("Lucas", false);
                        ReflectionTestUtils.setField(user, "id", 1L);
                        when(service.createUser("Lucas", false)).thenReturn(user);

                        mockMvc.perform(post("/api/users")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"name\":\"Lucas\",\"premium\":false}"))
                                        .andExpect(status().isOk());

                        verify(service).createUser("Lucas", false);
                }
        }

        @Nested
        @DisplayName("POST /api/trips")
        class CreateTripEndpoint {

                @Test
                @DisplayName("devrait appeler le service pour créer un trajet et retourner 200")
                void shouldCreateTrip() throws Exception {
                        Trip trip = new Trip("Balade Alpine", 5, false);
                        ReflectionTestUtils.setField(trip, "id", 1L);
                        when(service.createTrip("Balade Alpine", 5, false)).thenReturn(trip);

                        mockMvc.perform(post("/api/trips")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"name\":\"Balade Alpine\",\"maxParticipants\":5,\"premiumOnly\":false}"))
                                        .andExpect(status().isOk());

                        verify(service).createTrip("Balade Alpine", 5, false);
                }

                @Test
                @DisplayName("devrait lancer une exception si la capacité est invalide")
                void shouldThrowException_whenCapacityInvalid() throws Exception {
                        when(service.createTrip(anyString(), eq(0), anyBoolean()))
                                        .thenThrow(new IllegalArgumentException("Invalid capacity"));

                        ServletException ex = assertThrows(ServletException.class, () -> mockMvc.perform(post(
                                        "/api/trips")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"name\":\"Bad\",\"maxParticipants\":0,\"premiumOnly\":false}")));

                        assertTrue(ex.getCause() instanceof IllegalArgumentException);
                        assertEquals("Invalid capacity", ex.getCause().getMessage());
                }
        }

        @Nested
        @DisplayName("POST /api/trips/{id}/join")
        class JoinTripEndpoint {

                @Test
                @DisplayName("devrait appeler le service pour rejoindre un trajet et retourner 200")
                void shouldJoinTrip() throws Exception {
                        Trip trip = new Trip("Balade", 5, false);
                        ReflectionTestUtils.setField(trip, "id", 1L);
                        when(service.joinTrip(1L, 1L)).thenReturn(trip);

                        mockMvc.perform(post("/api/trips/1/join")
                                        .param("userId", "1"))
                                        .andExpect(status().isOk());

                        verify(service).joinTrip(1L, 1L);
                }

                @Test
                @DisplayName("devrait lancer une exception si le trajet n'existe pas")
                void shouldThrowException_whenTripNotFound() throws Exception {
                        when(service.joinTrip(eq(99L), anyLong()))
                                        .thenThrow(new RuntimeException("Trip not found"));

                        ServletException ex = assertThrows(ServletException.class,
                                        () -> mockMvc.perform(post("/api/trips/99/join")
                                                        .param("userId", "1")));

                        assertEquals("Trip not found", ex.getCause().getMessage());
                }
        }

        @Nested
        @DisplayName("POST /api/trips/{id}/start")
        class StartTripEndpoint {

                @Test
                @DisplayName("devrait appeler le service pour démarrer un trajet et retourner 200")
                void shouldStartTrip() throws Exception {
                        Trip trip = new Trip("Ready Ride", 5, false);
                        ReflectionTestUtils.setField(trip, "id", 1L);
                        when(service.startTrip(1L)).thenReturn(trip);

                        mockMvc.perform(post("/api/trips/1/start"))
                                        .andExpect(status().isOk());

                        verify(service).startTrip(1L);
                }
        }

        @Nested
        @DisplayName("GET /api/trips")
        class GetTripsEndpoint {

                @Test
                @DisplayName("devrait appeler le service pour récupérer la liste des trajets")
                void shouldReturnAllTrips() throws Exception {
                        Trip trip1 = new Trip("Balade 1", 5, false);
                        ReflectionTestUtils.setField(trip1, "id", 1L);

                        when(service.allTrips()).thenReturn(Arrays.asList(trip1));

                        // On vérifie que la requête passe et que c'est un tableau JSON,
                        // même si les objets dedans seront vides sans les Getters.
                        mockMvc.perform(get("/api/trips"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.length()").value(1));

                        verify(service).allTrips();
                }
        }
}