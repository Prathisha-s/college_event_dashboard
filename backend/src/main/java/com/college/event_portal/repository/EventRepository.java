package com.college.event_portal.repository;

import com.college.event_portal.model.Event;
import com.college.event_portal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    // Fetch events created by a specific faculty
    List<Event> findByFaculty(User faculty);
}
