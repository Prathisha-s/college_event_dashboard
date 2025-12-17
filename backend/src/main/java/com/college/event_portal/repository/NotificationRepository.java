package com.college.event_portal.repository;

import com.college.event_portal.model.Notification;
import com.college.event_portal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Fetch notifications for a student along with event data to avoid LazyInitializationException
    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.event WHERE n.student = :student ORDER BY n.sentAt DESC")
    List<Notification> findByStudentWithEvent(@Param("student") User student);
}
