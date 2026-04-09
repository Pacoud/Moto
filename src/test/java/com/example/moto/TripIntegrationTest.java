package com.example.moto;

import com.example.moto.Repository.TripRepository;
import com.example.moto.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TripIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @BeforeEach
    void cleanDatabase() {
        tripRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================================
    // Persistance User
    // =========================================================================

    @Nested
    @DisplayName("Persistance User")
    class UserPersistenceTests {

        @Test
        @DisplayName("devrait sauvegarder un utilisateur et le retrouver par son ID")
        void shouldSaveAndFindUser() {
            // Arrange
            User user = new User("Lucas", false);

            // Act
            User saved = userRepository.save(user);
            Long savedId = (Long) ReflectionTestUtils.getField(saved, "id");

            // Assert
            assertNotNull(savedId);
            Optional<User> found = userRepository.findById(savedId);
            assertTrue(found.isPresent());
            assertEquals("Lucas", ReflectionTestUtils.getField(found.get(), "name"));
            assertEquals(false, ReflectionTestUtils.getField(found.get(), "premium"));
            assertEquals(0, ReflectionTestUtils.getField(found.get(), "points"));
        }

        @Test
        @DisplayName("devrait sauvegarder un utilisateur premium")
        void shouldSavePremiumUser() {
            // Arrange & Act
            User user = new User("PremiumRider", true);
            User saved = userRepository.save(user);
            Long savedId = (Long) ReflectionTestUtils.getField(saved, "id");

            // Assert
            Optional<User> found = userRepository.findById(savedId);
            assertTrue(found.isPresent());
            assertEquals("PremiumRider", ReflectionTestUtils.getField(found.get(), "name"));
            assertEquals(true, ReflectionTestUtils.getField(found.get(), "premium"));
        }

        @Test
        @DisplayName("devrait persister les points après addPoints()")
        void shouldPersistPointsAfterAdd() {
            // Arrange
            User user = new User("Rider", false);
            User saved = userRepository.save(user);
            Long savedId = (Long) ReflectionTestUtils.getField(saved, "id");

            // Act
            saved.addPoints(10);
            userRepository.save(saved);

            // Assert
            User found = userRepository.findById(savedId).orElseThrow();
            assertEquals(10, ReflectionTestUtils.getField(found, "points"));
        }

        @Test
        @DisplayName("devrait retourner une liste vide quand la base est vide")
        void shouldReturnEmptyList_whenNoUsers() {
            // Act & Assert
            List<User> users = userRepository.findAll();
            assertTrue(users.isEmpty());
        }

        @Test
        @DisplayName("devrait retrouver tous les utilisateurs sauvegardés")
        void shouldFindAllUsers() {
            // Arrange
            userRepository.save(new User("Alice", false));
            userRepository.save(new User("Bob", true));
            userRepository.save(new User("Charlie", false));

            // Act
            List<User> users = userRepository.findAll();

            // Assert
            assertEquals(3, users.size());
        }

        @Test
        @DisplayName("devrait retourner Optional.empty pour un ID inexistant")
        void shouldReturnEmpty_whenUserNotFound() {
            // Act & Assert
            Optional<User> found = userRepository.findById(999L);
            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("devrait supprimer un utilisateur")
        void shouldDeleteUser() {
            // Arrange
            User user = new User("ToDelete", false);
            User saved = userRepository.save(user);
            Long savedId = (Long) ReflectionTestUtils.getField(saved, "id");

            // Act
            userRepository.deleteById(savedId);

            // Assert
            assertFalse(userRepository.findById(savedId).isPresent());
        }
    }

    // =========================================================================
    // Persistance Trip
    // =========================================================================

    @Nested
    @DisplayName("Persistance Trip")
    class TripPersistenceTests {

        @Test
        @DisplayName("devrait sauvegarder un trajet et le retrouver par son ID")
        void shouldSaveAndFindTrip() {
            // Arrange
            Trip trip = new Trip("Balade Alpine", 5, false);

            // Act
            Trip saved = tripRepository.save(trip);
            Long savedId = (Long) ReflectionTestUtils.getField(saved, "id");

            // Assert
            assertNotNull(savedId);
            Optional<Trip> found = tripRepository.findById(savedId);
            assertTrue(found.isPresent());
            assertEquals("Balade Alpine", ReflectionTestUtils.getField(found.get(), "name"));
            assertEquals(5, ReflectionTestUtils.getField(found.get(), "maxParticipants"));
            assertEquals(false, ReflectionTestUtils.getField(found.get(), "premiumOnly"));
            assertEquals(false, ReflectionTestUtils.getField(found.get(), "started"));
        }

        @Test
        @DisplayName("devrait sauvegarder un trajet premium")
        void shouldSavePremiumTrip() {
            // Arrange & Act
            Trip trip = new Trip("VIP Ride", 3, true);
            Trip saved = tripRepository.save(trip);
            Long savedId = (Long) ReflectionTestUtils.getField(saved, "id");

            // Assert
            Optional<Trip> found = tripRepository.findById(savedId);
            assertTrue(found.isPresent());
            assertEquals(true, ReflectionTestUtils.getField(found.get(), "premiumOnly"));
        }

        @Test
        @DisplayName("devrait persister l'état démarré après start()")
        void shouldPersistStartedState() {
            // Arrange
            User user = new User("Rider", false);
            userRepository.save(user);

            Trip trip = new Trip("Ready Ride", 5, false);
            trip.join(user);
            Trip saved = tripRepository.save(trip);
            Long savedId = (Long) ReflectionTestUtils.getField(saved, "id");

            // Act
            saved.start();
            tripRepository.save(saved);

            // Assert
            Trip found = tripRepository.findById(savedId).orElseThrow();
            assertEquals(true, ReflectionTestUtils.getField(found, "started"));
        }

        @Test
        @DisplayName("devrait retrouver tous les trajets")
        void shouldFindAllTrips() {
            // Arrange
            tripRepository.save(new Trip("Trip 1", 5, false));
            tripRepository.save(new Trip("Trip 2", 3, true));

            // Act
            List<Trip> trips = tripRepository.findAll();

            // Assert
            assertEquals(2, trips.size());
        }

        @Test
        @DisplayName("devrait retourner Optional.empty pour un trip inexistant")
        void shouldReturnEmpty_whenTripNotFound() {
            // Act & Assert
            assertFalse(tripRepository.findById(999L).isPresent());
        }
    }

    // =========================================================================
    // Relations User/Trip (OneToMany participants)
    // =========================================================================

    @Nested
    @DisplayName("Relations User/Trip")
    class UserTripRelationTests {

        @Test
        @DisplayName("devrait persister la relation Trip -> User (participants)")
        void shouldPersistTripWithParticipant() {
            // Arrange
            User user = new User("Rider", false);
            userRepository.save(user);

            Trip trip = new Trip("Balade", 5, false);
            trip.join(user);

            // Act
            Trip saved = tripRepository.save(trip);
            Long savedId = (Long) ReflectionTestUtils.getField(saved, "id");

            // Assert
            Trip found = tripRepository.findById(savedId).orElseThrow();
            @SuppressWarnings("unchecked")
            List<User> participants = (List<User>) ReflectionTestUtils.getField(found, "participants");
            assertEquals(1, participants.size());
            assertEquals("Rider", ReflectionTestUtils.getField(participants.get(0), "name"));
        }

        @Test
        @DisplayName("devrait persister plusieurs participants dans un trajet")
        void shouldPersistMultipleParticipants() {
            // Arrange
            User alice = new User("Alice", false);
            User bob = new User("Bob", false);
            User charlie = new User("Charlie", false);
            userRepository.save(alice);
            userRepository.save(bob);
            userRepository.save(charlie);

            Trip trip = new Trip("Group Ride", 5, false);
            trip.join(alice);
            trip.join(bob);
            trip.join(charlie);

            // Act
            Trip saved = tripRepository.save(trip);
            Long savedId = (Long) ReflectionTestUtils.getField(saved, "id");

            // Assert
            Trip found = tripRepository.findById(savedId).orElseThrow();
            @SuppressWarnings("unchecked")
            List<User> participants = (List<User>) ReflectionTestUtils.getField(found, "participants");
            assertEquals(3, participants.size());
        }

        @Test
        @DisplayName("devrait persister les points des participants après join")
        void shouldPersistUserPointsAfterJoin() {
            // Arrange
            User user = new User("Rider", false);
            userRepository.save(user);

            Trip trip = new Trip("Balade", 5, false);
            trip.join(user); // +10 points

            // Act
            userRepository.save(user);
            tripRepository.save(trip);

            Long userId = (Long) ReflectionTestUtils.getField(user, "id");

            // Assert
            User found = userRepository.findById(userId).orElseThrow();
            assertEquals(10, ReflectionTestUtils.getField(found, "points"));
        }

        @Test
        @DisplayName("un trajet vide devrait avoir une liste de participants vide")
        void shouldHaveEmptyParticipantsList_whenNoOneJoined() {
            // Arrange & Act
            Trip trip = new Trip("Empty Ride", 5, false);
            Trip saved = tripRepository.save(trip);
            Long savedId = (Long) ReflectionTestUtils.getField(saved, "id");

            // Assert
            Trip found = tripRepository.findById(savedId).orElseThrow();
            @SuppressWarnings("unchecked")
            List<User> participants = (List<User>) ReflectionTestUtils.getField(found, "participants");
            assertTrue(participants.isEmpty());
        }

        @Test
        @DisplayName("remainingPlaces devrait refléter les participants persistés")
        void shouldReflectRemainingPlacesAfterPersist() {
            // Arrange
            User u1 = new User("A", false);
            User u2 = new User("B", false);
            userRepository.save(u1);
            userRepository.save(u2);

            Trip trip = new Trip("Ride", 5, false);
            trip.join(u1);
            trip.join(u2);

            // Act
            Trip saved = tripRepository.save(trip);
            Long savedId = (Long) ReflectionTestUtils.getField(saved, "id");
            Trip found = tripRepository.findById(savedId).orElseThrow();

            // Assert
            assertEquals(3, found.remainingPlaces());
        }

        @Test
        @DisplayName("un utilisateur premium rejoint un trip premium — relation persistée")
        void shouldPersistPremiumUserInPremiumTrip() {
            // Arrange
            User premiumUser = new User("VIPRider", true);
            userRepository.save(premiumUser);

            Trip premiumTrip = new Trip("VIP Ride", 3, true);
            premiumTrip.join(premiumUser);

            // Act
            Trip saved = tripRepository.save(premiumTrip);
            Long savedId = (Long) ReflectionTestUtils.getField(saved, "id");

            // Assert
            Trip found = tripRepository.findById(savedId).orElseThrow();
            @SuppressWarnings("unchecked")
            List<User> participants = (List<User>) ReflectionTestUtils.getField(found, "participants");
            assertEquals(1, participants.size());
            assertEquals(true, ReflectionTestUtils.getField(participants.get(0), "premium"));
        }
    }
}
