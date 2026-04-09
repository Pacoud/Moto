package com.example.moto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Trip {
    @Id
    @GeneratedValue
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
        if (started)
            throw new RuntimeException("Trip already started");

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

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }
    public boolean isPremiumOnly() { return premiumOnly; }
    public void setPremiumOnly(boolean premiumOnly) { this.premiumOnly = premiumOnly; }
    public boolean isStarted() { return started; }
    public void setStarted(boolean started) { this.started = started; }
    public List<User> getParticipants() { return participants; }
    public void setParticipants(List<User> participants) { this.participants = participants; }
}