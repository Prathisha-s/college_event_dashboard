package com.college.event_portal.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;  // e.g. "Registered", "Approved", "Rejected"
    private String details; // extra info like "Student Sri registered for Dance"
    private LocalDateTime timestamp;

    @ManyToOne
    private User user;

    @ManyToOne
    private Event event;

    // ---------------- Constructors ----------------
    public Activity() {} // default constructor

    // Constructor for your notification logging use case
    public Activity(User user, String details, LocalDateTime timestamp) {
        this.user = user;
        this.details = details;
        this.timestamp = timestamp;
        this.action = "Notification"; // default action, can be overridden if needed
    }

    // Full constructor with event and action
    public Activity(User user, Event event, String action, String details, LocalDateTime timestamp) {
        this.user = user;
        this.event = event;
        this.action = action;
        this.details = details;
        this.timestamp = timestamp;
    }

    // ---------------- Getters & Setters ----------------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
}
