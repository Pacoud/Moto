package com.example.moto;

import com.example.moto.Repository.TripRepository;
import com.example.moto.Repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MotoTripE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void cleanDatabase() {
        tripRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================================
    // Helpers — créer des entités via l'API HTTP et récupérer l'ID en base
    // =========================================================================

    private Long createUserViaApi(String name, boolean premium) throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"premium\":" + premium + "}"))
                .andExpect(status().isOk());

        return userRepository.findAll().stream()
                .filter(u -> name.equals(ReflectionTestUtils.getField(u, "name")))
                .map(u -> (Long) ReflectionTestUtils.getField(u, "id"))
                .findFirst()
                .orElseThrow();
    }

    private Long createTripViaApi(String name, int maxParticipants, boolean premiumOnly) throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"maxParticipants\":" + maxParticipants
                                + ",\"premiumOnly\":" + premiumOnly + "}"))
                .andExpect(status().isOk());

        return tripRepository.findAll().stream()
                .filter(t -> name.equals(ReflectionTestUtils.getField(t, "name")))
                .map(t -> (Long) ReflectionTestUtils.getField(t, "id"))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Charge un Trip dans une transaction ouverte pour pouvoir accéder
     * aux collections LAZY (participants) via ReflectionTestUtils.
     */
    @SuppressWarnings("unchecked")
    private List<User> getParticipantsInTransaction(Long tripId) {
        return transactionTemplate.execute(status -> {
            Trip trip = tripRepository.findById(tripId).orElseThrow();
            List<User> participants = (List<User>) ReflectionTestUtils.getField(trip, "participants");
            // Forcer le chargement de la collection LAZY
            participants.size();
            return List.copyOf(participants);
        });
    }

    // =========================================================================
    // Golden Path — Scénario complet
    // =========================================================================

    @Nested
    @DisplayName("Golden Path — Scénario complet")
    class GoldenPath {

        @Test
        @DisplayName("Créer user → Créer trip → Join → Start → vérifier started=true et points=10")
        void fullScenario() throws Exception {
            // 1. Créer un utilisateur via l'API
            Long userId = createUserViaApi("Lucas", false);

            // 2. Créer un trajet via l'API
            Long tripId = createTripViaApi("Balade Alpine", 5, false);

            // 3. L'utilisateur rejoint le trajet
            mockMvc.perform(post("/api/trips/" + tripId + "/join")
                            .param("userId", userId.toString()))
                    .andExpect(status().isOk());

            // 4. Démarrer le trajet
            mockMvc.perform(post("/api/trips/" + tripId + "/start"))
                    .andExpect(status().isOk());

            // 5. Vérifications en base de données
            transactionTemplate.executeWithoutResult(status -> {
                Trip trip = tripRepository.findById(tripId).orElseThrow();
                assertEquals(true, ReflectionTestUtils.getField(trip, "started"));

                User user = userRepository.findById(userId).orElseThrow();
                assertEquals(10, ReflectionTestUtils.getField(user, "points"));
            });
        }
    }

    // =========================================================================
    // Cas d'erreurs
    // =========================================================================

    @Nested
    @DisplayName("Cas d'erreurs E2E")
    class ErrorCases {

        @Test
        @DisplayName("Trip full — rejoindre un trajet déjà plein doit échouer")
        void shouldFail_whenTripIsFull() throws Exception {
            // Créer un trip de 1 place
            Long tripId = createTripViaApi("Petit Ride", 1, false);

            // Créer 2 utilisateurs
            Long userId1 = createUserViaApi("Alice", false);
            Long userId2 = createUserViaApi("Bob", false);

            // Le premier rejoint — OK
            mockMvc.perform(post("/api/trips/" + tripId + "/join")
                            .param("userId", userId1.toString()))
                    .andExpect(status().isOk());

            // Le deuxième rejoint — doit échouer (trip full)
            ServletException ex = assertThrows(ServletException.class,
                    () -> mockMvc.perform(post("/api/trips/" + tripId + "/join")
                            .param("userId", userId2.toString())));

            assertTrue(ex.getCause() instanceof RuntimeException);
            assertEquals("Trip full", ex.getCause().getMessage());

            // Vérifier en base : toujours 1 seul participant
            List<User> participants = getParticipantsInTransaction(tripId);
            assertEquals(1, participants.size());
        }

        @Test
        @DisplayName("Premium refusé — utilisateur normal sur trip premium doit échouer")
        void shouldFail_whenNonPremiumUserJoinsPremiumTrip() throws Exception {
            // Créer un trip premium
            Long tripId = createTripViaApi("VIP Ride", 5, true);

            // Créer un utilisateur NON premium
            Long userId = createUserViaApi("RegularRider", false);

            // Tentative de join — doit échouer
            ServletException ex = assertThrows(ServletException.class,
                    () -> mockMvc.perform(post("/api/trips/" + tripId + "/join")
                            .param("userId", userId.toString())));

            assertTrue(ex.getCause() instanceof RuntimeException);
            assertEquals("Premium required", ex.getCause().getMessage());

            // Vérifier en base : aucun participant
            List<User> participants = getParticipantsInTransaction(tripId);
            assertTrue(participants.isEmpty());
        }

        @Test
        @DisplayName("Trip déjà démarré — rejoindre après start doit échouer")
        void shouldFail_whenTripAlreadyStarted() throws Exception {
            // Créer user + trip, join, start
            Long userId1 = createUserViaApi("Rider1", false);
            Long tripId = createTripViaApi("Started Ride", 5, false);

            mockMvc.perform(post("/api/trips/" + tripId + "/join")
                            .param("userId", userId1.toString()))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/trips/" + tripId + "/start"))
                    .andExpect(status().isOk());

            // Créer un second utilisateur et tenter de rejoindre
            Long userId2 = createUserViaApi("LateRider", false);

            ServletException ex = assertThrows(ServletException.class,
                    () -> mockMvc.perform(post("/api/trips/" + tripId + "/join")
                            .param("userId", userId2.toString())));

            assertTrue(ex.getCause() instanceof RuntimeException);
            assertEquals("Trip already started", ex.getCause().getMessage());

            // Vérifier en base : toujours 1 seul participant
            List<User> participants = getParticipantsInTransaction(tripId);
            assertEquals(1, participants.size());
        }

        @Test
        @DisplayName("User inexistant — rejoindre avec un userId invalide doit échouer")
        void shouldFail_whenUserDoesNotExist() throws Exception {
            Long tripId = createTripViaApi("Ghost Ride", 5, false);

            ServletException ex = assertThrows(ServletException.class,
                    () -> mockMvc.perform(post("/api/trips/" + tripId + "/join")
                            .param("userId", "99999")));

            assertTrue(ex.getCause() instanceof RuntimeException);
            assertEquals("User not found", ex.getCause().getMessage());
        }

        @Test
        @DisplayName("Trip inexistant — rejoindre un tripId invalide doit échouer")
        void shouldFail_whenTripDoesNotExist() throws Exception {
            Long userId = createUserViaApi("LostRider", false);

            ServletException ex = assertThrows(ServletException.class,
                    () -> mockMvc.perform(post("/api/trips/99999/join")
                            .param("userId", userId.toString())));

            assertTrue(ex.getCause() instanceof RuntimeException);
            assertEquals("Trip not found", ex.getCause().getMessage());
        }

        @Test
        @DisplayName("Capacité invalide — créer un trip avec maxParticipants <= 0 doit échouer")
        void shouldFail_whenCapacityIsInvalid() throws Exception {
            ServletException ex = assertThrows(ServletException.class,
                    () -> mockMvc.perform(post("/api/trips")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Bad Trip\",\"maxParticipants\":0,\"premiumOnly\":false}")));

            assertTrue(ex.getCause() instanceof IllegalArgumentException);
            assertEquals("Invalid capacity", ex.getCause().getMessage());

            // Vérifier en base : aucun trip créé
            assertTrue(tripRepository.findAll().isEmpty());
        }
    }
}
