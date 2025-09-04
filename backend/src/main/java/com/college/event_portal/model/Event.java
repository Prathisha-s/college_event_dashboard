package com.college.event_portal.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private LocalDate date;
    private String venue;

    private String email;
    private String hostedBy;
    private String sponsor;
    private LocalTime startTime;
    private String startAmPm;
    private LocalTime endTime;
    private String endAmPm;
    private String category;

    @ManyToOne
    private User faculty; // Link to faculty creating the event

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getHostedBy() { return hostedBy; }
    public void setHostedBy(String hostedBy) { this.hostedBy = hostedBy; }

    public String getSponsor() { return sponsor; }
    public void setSponsor(String sponsor) { this.sponsor = sponsor; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public String getStartAmPm() { return startAmPm; }
    public void setStartAmPm(String startAmPm) { this.startAmPm = startAmPm; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getEndAmPm() { return endAmPm; }
    public void setEndAmPm(String endAmPm) { this.endAmPm = endAmPm; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public User getFaculty() { return faculty; }
    public void setFaculty(User faculty) { this.faculty = faculty; }
}
