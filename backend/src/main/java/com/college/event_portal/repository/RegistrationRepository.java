package com.college.event_portal.repository;

import com.college.event_portal.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    // Fetch registrations along with their associated event
    @Query("SELECT r FROM Registration r " +
           "JOIN FETCH r.event e " +
           "WHERE r.student.id = :studentId " +
           "ORDER BY r.registrationTime DESC")
    List<Registration> findByStudentIdWithEvent(@Param("studentId") Long studentId);

    // Fetch registrations for a specific event
    @Query("SELECT r FROM Registration r WHERE r.event.id = :eventId")
    List<Registration> findByEventId(@Param("eventId") Long eventId);
}
