# moto-trip

## Contexte de l’application : MotoTrip

Vous travaillez sur une application appelée MotoTrip, une plateforme dédiée à l’organisation de balades moto entre passionnés.

L’objectif de cette application est de permettre à des utilisateurs de créer et rejoindre des trajets (trips), tout en respectant certaines règles métier liées à la capacité, au statut premium et au déroulement des événements.

---

## Gestion des utilisateurs

Un utilisateur possède :
- un nom
- un statut premium (oui/non)
- un nombre de points

Les points sont attribués automatiquement lorsqu’un utilisateur participe à un trajet.

---

## Gestion des trajets (Trips)

Un trajet est défini par :
- un nom
- une capacité maximale de participants
- une restriction éventuelle aux utilisateurs premium
- un état (démarré ou non)
- une liste de participants

---

## Fonctionnalités principales

L’application permet de :

### 1. Créer des utilisateurs
- avec ou sans statut premium

### 2. Créer des trajets
- avec une capacité définie
- éventuellement réservés aux utilisateurs premium

### 3. Rejoindre un trajet
- un utilisateur peut rejoindre un trajet si :
  - le trajet n’est pas complet
  - le trajet n’a pas déjà commencé
  - les conditions premium sont respectées
- lorsqu’un utilisateur rejoint un trajet, il gagne des points

### 4. Démarrer un trajet
- un trajet peut être démarré uniquement s’il contient au moins un participant
- une fois démarré, aucun nouvel utilisateur ne peut rejoindre

### 5. Consulter les trajets
- récupérer la liste des trajets existants

---

## Règles métier importantes

- Un trajet ne peut pas dépasser sa capacité maximale
- Un utilisateur non premium ne peut pas rejoindre un trajet premium
- Un trajet démarré est verrouillé (plus de participants)
- Un trajet sans participant ne peut pas être démarré
- Une capacité de trajet invalide (≤ 0) doit être rejetée
- Un utilisateur ou un trajet inexistant doit générer une erreur

---

## Objectif de l’examen

Le code de l’application vous est fourni.

Votre objectif est de concevoir et implémenter une stratégie de test complète, couvrant :

- les tests unitaires
- les tests d’intégration (avec base H2)
- les tests API REST
- les tests end-to-end (E2E)

Vous ne devez pas modifier le code métier, uniquement écrire les tests.

---





## EXAMEN TECHNIQUE - 2H30

- Application : MotoTrip 
- Objectif : écrire TOUS les tests (unitaires, intégration, REST, E2E)
- NE PAS modifier le code métier


 DEPENDENCIES (pom.xml)

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```


ETITIES

```java
@Entity
public class User {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private boolean premium;
    private int points;

    public User() {}

    public User(String name, boolean premium) {
        this.name = name;
        this.premium = premium;
        this.points = 0;
    }

    public void addPoints(int pts) {
        this.points += pts;
    }

    public boolean canJoinPremium() {
        return premium;
    }
}
```

```java
@Entity
public class Trip {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private int maxParticipants;
    private boolean premiumOnly;
    private boolean started;

    @OneToMany
    private List<User> participants = new ArrayList<>();

    public Trip() {}

    public Trip(String name, int maxParticipants, boolean premiumOnly) {
        this.name = name;
        this.maxParticipants = maxParticipants;
        this.premiumOnly = premiumOnly;
        this.started = false;
    }

    public void join(User user) {
        if (started) throw new RuntimeException("Trip already started");

        if (premiumOnly && !user.canJoinPremium()) {
            throw new RuntimeException("Premium required");
        }

        if (participants.size() >= maxParticipants) {
            throw new RuntimeException("Trip full");
        }

        participants.add(user);
        user.addPoints(10);
    }

    public void start() {
        if (participants.isEmpty()) {
            throw new RuntimeException("No participants");
        }
        this.started = true;
    }

    public int remainingPlaces() {
        return maxParticipants - participants.size();
    }
}
```


REPOSITORIES

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {}

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {}
```

SERVICE


```java
@Service
public class TripService {

    private final TripRepository tripRepo;
    private final UserRepository userRepo;

    public TripService(TripRepository tripRepo, UserRepository userRepo) {
        this.tripRepo = tripRepo;
        this.userRepo = userRepo;
    }

    public Trip createTrip(String name, int max, boolean premiumOnly) {
        if (max <= 0) throw new IllegalArgumentException("Invalid capacity");
        return tripRepo.save(new Trip(name, max, premiumOnly));
    }

    public User createUser(String name, boolean premium) {
        return userRepo.save(new User(name, premium));
    }

    public Trip joinTrip(Long tripId, Long userId) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        trip.join(user);
        return tripRepo.save(trip);
    }

    public Trip startTrip(Long id) {
        Trip trip = tripRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        trip.start();
        return tripRepo.save(trip);
    }

    public List<Trip> allTrips() {
        return tripRepo.findAll();
    }
}
```

CONTROLLER


```java
@RestController
@RequestMapping("/api")
public class TripController {

    private final TripService service;

    public TripController(TripService service) {
        this.service = service;
    }

    @PostMapping("/users")
    public User createUser(@RequestBody Map<String, Object> p) {
        return service.createUser((String)p.get("name"), (boolean)p.get("premium"));
    }

    @PostMapping("/trips")
    public Trip createTrip(@RequestBody Map<String, Object> p) {
        return service.createTrip(
            (String)p.get("name"),
            (int)p.get("maxParticipants"),
            (boolean)p.get("premiumOnly")
        );
    }

    @PostMapping("/trips/{id}/join")
    public Trip join(@PathVariable Long id, @RequestParam Long userId) {
        return service.joinTrip(id, userId);
    }

    @PostMapping("/trips/{id}/start")
    public Trip start(@PathVariable Long id) {
        return service.startTrip(id);
    }

    @GetMapping("/trips")
    public List<Trip> all() {
        return service.allTrips();
    }
}
```


H2 CONFIG

```yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: create-drop
```

 SUJET ETUDIANTS

 Implémenter TOUS les tests :

 1. UNITAIRES
 - User (points, premium)
 - Trip (join, start, règles)

 2. SERVICE (MOCKITO)
 - createTrip
 - joinTrip
 - erreurs (user/trip inexistants)

 3. INTEGRATION (H2)
 - persistance
 - relations User/Trip

 4. TEST REST (MockMvc)
 - POST /users
 - POST /trips
 - POST /join
 - POST /start
 - GET /trips

 5. E2E
 scénario :
 create user -> create trip -> join -> start -> vérifier état

 CAS OBLIGATOIRES
 - trip full
 - premium refusé
 - trip déjà démarré
 - user inexistant
 - trip inexistant
 - capacité invalide

 BONUS
 - tests paramétrés
 - couverture branches
 - test concurrence




