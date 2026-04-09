package com.example.moto.Services;

import com.example.moto.Trip;
import com.example.moto.User;
import com.example.moto.Repository.TripRepository;
import com.example.moto.Repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepo;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private TripService tripService;

    // =========================================================================
    // createTrip
    // =========================================================================

    @Nested
    @DisplayName("createTrip")
    class CreateTripTests {

        @Test
        @DisplayName("devrait créer un trajet avec des paramètres valides")
        void shouldCreateTrip_whenValidParameters() {
            // Arrange
            Trip savedTrip = new Trip("Balade Alpine", 5, false);
            savedTrip.setId(1L);
            when(tripRepo.save(any(Trip.class))).thenReturn(savedTrip);

            // Act
            Trip result = tripService.createTrip("Balade Alpine", 5, false);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Balade Alpine", result.getName());
            assertEquals(5, result.getMaxParticipants());
            assertFalse(result.isPremiumOnly());
            assertFalse(result.isStarted());
            verify(tripRepo, times(1)).save(any(Trip.class));
        }

        @Test
        @DisplayName("devrait créer un trajet premium")
        void shouldCreatePremiumTrip() {
            // Arrange
            Trip savedTrip = new Trip("VIP Ride", 3, true);
            savedTrip.setId(2L);
            when(tripRepo.save(any(Trip.class))).thenReturn(savedTrip);

            // Act
            Trip result = tripService.createTrip("VIP Ride", 3, true);

            // Assert
            assertNotNull(result);
            assertTrue(result.isPremiumOnly());
            verify(tripRepo).save(any(Trip.class));
        }

        @Test
        @DisplayName("devrait lancer IllegalArgumentException si capacité <= 0")
        void shouldThrowException_whenCapacityIsZero() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> tripService.createTrip("Bad Trip", 0, false)
            );

            assertEquals("Invalid capacity", exception.getMessage());
            verify(tripRepo, never()).save(any(Trip.class));
        }

        @Test
        @DisplayName("devrait lancer IllegalArgumentException si capacité négative")
        void shouldThrowException_whenCapacityIsNegative() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> tripService.createTrip("Bad Trip", -5, false)
            );

            assertEquals("Invalid capacity", exception.getMessage());
            verify(tripRepo, never()).save(any(Trip.class));
        }
    }

    // =========================================================================
    // joinTrip
    // =========================================================================

    @Nested
    @DisplayName("joinTrip")
    class JoinTripTests {

        @Test
        @DisplayName("devrait permettre à un utilisateur de rejoindre un trajet avec succès")
        void shouldJoinTrip_whenAllConditionsMet() {
            // Arrange
            User user = new User("Lucas", false);
            user.setId(1L);

            Trip trip = new Trip("Balade Côtière", 5, false);
            trip.setId(1L);

            when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));
            when(userRepo.findById(1L)).thenReturn(Optional.of(user));
            when(tripRepo.save(any(Trip.class))).thenReturn(trip);

            // Act
            Trip result = tripService.joinTrip(1L, 1L);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getParticipants().size());
            assertEquals(10, user.getPoints()); // L'utilisateur gagne 10 points
            verify(tripRepo).findById(1L);
            verify(userRepo).findById(1L);
            verify(tripRepo).save(trip);
        }

        @Test
        @DisplayName("un utilisateur premium peut rejoindre un trajet premium")
        void shouldAllowPremiumUser_toJoinPremiumTrip() {
            // Arrange
            User premiumUser = new User("PremiumRider", true);
            premiumUser.setId(1L);

            Trip premiumTrip = new Trip("VIP Ride", 3, true);
            premiumTrip.setId(1L);

            when(tripRepo.findById(1L)).thenReturn(Optional.of(premiumTrip));
            when(userRepo.findById(1L)).thenReturn(Optional.of(premiumUser));
            when(tripRepo.save(any(Trip.class))).thenReturn(premiumTrip);

            // Act
            Trip result = tripService.joinTrip(1L, 1L);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getParticipants().size());
            assertTrue(premiumUser.isPremium());
        }

        @Test
        @DisplayName("devrait lancer RuntimeException si le trajet n'existe pas")
        void shouldThrowException_whenTripNotFound() {
            // Arrange
            when(tripRepo.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> tripService.joinTrip(99L, 1L)
            );

            assertEquals("Trip not found", exception.getMessage());
            verify(tripRepo).findById(99L);
            verify(userRepo, never()).findById(anyLong());
            verify(tripRepo, never()).save(any(Trip.class));
        }

        @Test
        @DisplayName("devrait lancer RuntimeException si l'utilisateur n'existe pas")
        void shouldThrowException_whenUserNotFound() {
            // Arrange
            Trip trip = new Trip("Balade", 5, false);
            trip.setId(1L);

            when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));
            when(userRepo.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> tripService.joinTrip(1L, 99L)
            );

            assertEquals("User not found", exception.getMessage());
            verify(tripRepo).findById(1L);
            verify(userRepo).findById(99L);
            verify(tripRepo, never()).save(any(Trip.class));
        }

        @Test
        @DisplayName("devrait lancer RuntimeException si le trajet est complet")
        void shouldThrowException_whenTripIsFull() {
            // Arrange
            Trip trip = new Trip("Mini Ride", 1, false);
            trip.setId(1L);

            User existingUser = new User("Existing", false);
            existingUser.setId(1L);
            trip.join(existingUser); // Le trajet est maintenant complet (1/1)

            User newUser = new User("NewRider", false);
            newUser.setId(2L);

            when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));
            when(userRepo.findById(2L)).thenReturn(Optional.of(newUser));

            // Act & Assert
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> tripService.joinTrip(1L, 2L)
            );

            assertEquals("Trip full", exception.getMessage());
            verify(tripRepo, never()).save(any(Trip.class));
        }

        @Test
        @DisplayName("devrait lancer RuntimeException si un non-premium tente de rejoindre un trajet premium")
        void shouldThrowException_whenNonPremiumJoinsPremiumTrip() {
            // Arrange
            Trip premiumTrip = new Trip("VIP Only", 5, true);
            premiumTrip.setId(1L);

            User nonPremiumUser = new User("BasicRider", false);
            nonPremiumUser.setId(1L);

            when(tripRepo.findById(1L)).thenReturn(Optional.of(premiumTrip));
            when(userRepo.findById(1L)).thenReturn(Optional.of(nonPremiumUser));

            // Act & Assert
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> tripService.joinTrip(1L, 1L)
            );

            assertEquals("Premium required", exception.getMessage());
            verify(tripRepo, never()).save(any(Trip.class));
        }

        @Test
        @DisplayName("devrait lancer RuntimeException si le trajet a déjà démarré")
        void shouldThrowException_whenTripAlreadyStarted() {
            // Arrange
            Trip trip = new Trip("Started Ride", 5, false);
            trip.setId(1L);

            // Ajouter un participant puis démarrer le trajet
            User participant = new User("First", false);
            participant.setId(10L);
            trip.join(participant);
            trip.start();

            User lateUser = new User("LateRider", false);
            lateUser.setId(2L);

            when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));
            when(userRepo.findById(2L)).thenReturn(Optional.of(lateUser));

            // Act & Assert
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> tripService.joinTrip(1L, 2L)
            );

            assertEquals("Trip already started", exception.getMessage());
            verify(tripRepo, never()).save(any(Trip.class));
        }
    }

    // =========================================================================
    // startTrip
    // =========================================================================

    @Nested
    @DisplayName("startTrip")
    class StartTripTests {

        @Test
        @DisplayName("devrait démarrer un trajet avec au moins un participant")
        void shouldStartTrip_whenHasParticipants() {
            // Arrange
            Trip trip = new Trip("Ready Ride", 5, false);
            trip.setId(1L);

            User user = new User("Rider", false);
            user.setId(1L);
            trip.join(user); // Ajouter un participant

            when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));
            when(tripRepo.save(any(Trip.class))).thenReturn(trip);

            // Act
            Trip result = tripService.startTrip(1L);

            // Assert
            assertNotNull(result);
            assertTrue(result.isStarted());
            verify(tripRepo).findById(1L);
            verify(tripRepo).save(trip);
        }

        @Test
        @DisplayName("devrait lancer RuntimeException si le trajet n'a aucun participant")
        void shouldThrowException_whenNoParticipants() {
            // Arrange
            Trip emptyTrip = new Trip("Empty Ride", 5, false);
            emptyTrip.setId(1L);

            when(tripRepo.findById(1L)).thenReturn(Optional.of(emptyTrip));

            // Act & Assert
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> tripService.startTrip(1L)
            );

            assertEquals("No participants", exception.getMessage());
            verify(tripRepo).findById(1L);
            verify(tripRepo, never()).save(any(Trip.class));
        }

        @Test
        @DisplayName("devrait lancer RuntimeException si le trajet n'existe pas")
        void shouldThrowException_whenTripNotFound() {
            // Arrange
            when(tripRepo.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> tripService.startTrip(99L)
            );

            assertEquals("Trip not found", exception.getMessage());
            verify(tripRepo).findById(99L);
            verify(tripRepo, never()).save(any(Trip.class));
        }
    }
}
