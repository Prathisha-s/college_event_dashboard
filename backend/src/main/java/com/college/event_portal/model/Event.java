package com.college.event_portal.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private LocalDate date;

    private String venue;
    private String email;
    private String hostedBy;
    private String sponsor;

    private LocalTime startTime;
    private LocalTime endTime;

    private String category;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "faculty_id")
    private User faculty;

    @Transient
    private Integer startHour;
    @Transient
    private String startMinute;
    @Transient
    private String startAmPm;
    @Transient
    private Integer endHour;
    @Transient
    private String endMinute;
    @Transient
    private String endAmPm;

    // Transient field for registration count
    @Transient
    private int registrationCount;

    public Event() {}

    // --- Getters and Setters ---
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

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public User getFaculty() { return faculty; }
    public void setFaculty(User faculty) { this.faculty = faculty; }

    public Integer getStartHour() { return startHour; }
    public void setStartHour(Integer startHour) { this.startHour = startHour; }

    public String getStartMinute() { return startMinute; }
    public void setStartMinute(String startMinute) { this.startMinute = startMinute; }

    public String getStartAmPm() { return startAmPm; }
    public void setStartAmPm(String startAmPm) { this.startAmPm = startAmPm; }

    public Integer getEndHour() { return endHour; }
    public void setEndHour(Integer endHour) { this.endHour = endHour; }

    public String getEndMinute() { return endMinute; }
    public void setEndMinute(String endMinute) { this.endMinute = endMinute; }

    public String getEndAmPm() { return endAmPm; }
    public void setEndAmPm(String endAmPm) { this.endAmPm = endAmPm; }

    public int getRegistrationCount() { return registrationCount; }
    public void setRegistrationCount(int registrationCount) { this.registrationCount = registrationCount; }
}
    