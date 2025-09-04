package com.college.event_portal.controller;

import com.college.event_portal.model.Event;
import com.college.event_portal.model.Registration;
import com.college.event_portal.model.User;
import com.college.event_portal.repository.EventRepository;
import com.college.event_portal.repository.RegistrationRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@Controller
public class FacultyController {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository; // added

    // ================= Dashboard =================
    @GetMapping("/faculty/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");

        if (user != null) {
            model.addAttribute("name", user.getName());
            model.addAttribute("role", user.getRole());
            model.addAttribute("currentPage", "dashboard");

            List<Event> events = eventRepository.findByFaculty(user);
            model.addAttribute("events", events);
        }
        return "faculty/dashboard";
    }

    // ================= Create Event =================
    @GetMapping("/faculty/create-event")
    public String createEvent(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user != null) {
            model.addAttribute("name", user.getName());
            model.addAttribute("role", user.getRole());
            model.addAttribute("currentPage", "create-event");
            model.addAttribute("event", new Event());
        }
        return "faculty/create-event";
    }

    @PostMapping("/faculty/create-event")
    public String saveEvent(@ModelAttribute("event") Event event, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";

        event.setFaculty(user);

        // Adjust start time for AM/PM
        if (event.getStartTime() != null && event.getStartAmPm() != null && !event.getStartAmPm().isEmpty()) {
            int hour = event.getStartTime().getHour();
            if ("PM".equalsIgnoreCase(event.getStartAmPm()) && hour < 12) hour += 12;
            else if ("AM".equalsIgnoreCase(event.getStartAmPm()) && hour == 12) hour = 0;
            event.setStartTime(LocalTime.of(hour, event.getStartTime().getMinute()));
        }

        // Adjust end time for AM/PM
        if (event.getEndTime() != null && event.getEndAmPm() != null && !event.getEndAmPm().isEmpty()) {
            int hour = event.getEndTime().getHour();
            if ("PM".equalsIgnoreCase(event.getEndAmPm()) && hour < 12) hour += 12;
            else if ("AM".equalsIgnoreCase(event.getEndAmPm()) && hour == 12) hour = 0;
            event.setEndTime(LocalTime.of(hour, event.getEndTime().getMinute()));
        }

        eventRepository.save(event);
        return "redirect:/faculty/dashboard";
    }

    // ================= Manage Events =================
    @GetMapping("/faculty/manage-events")
    public String manageEvents(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user != null) {
            model.addAttribute("name", user.getName());
            model.addAttribute("role", user.getRole());
            model.addAttribute("currentPage", "manage-events");
            List<Event> events = eventRepository.findByFaculty(user);
            model.addAttribute("events", events);
        }
        return "faculty/manage-events";
    }

    @PostMapping("/faculty/delete-event/{id}")
    public String deleteEvent(@PathVariable("id") Long id, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";

        Event event = eventRepository.findById(id).orElse(null);
        if (event != null && event.getFaculty().getId().equals(user.getId())) {
            eventRepository.delete(event);
        }
        return "redirect:/faculty/manage-events";
    }

    // ================= View Registrations =================
    // ================= View Registrations =================
@GetMapping("/faculty/registrations/{eventId}")
public String viewRegistrations(@PathVariable("eventId") Long eventId,
                                HttpSession session,
                                Model model) {
    // Get logged-in faculty
    User user = (User) session.getAttribute("currentUser");
    if (user == null) return "redirect:/login";

    // Find the event
    Event event = eventRepository.findById(eventId).orElse(null);
    if (event == null || !event.getFaculty().getId().equals(user.getId())) {
        return "redirect:/faculty/manage-events"; // security check
    }

    // Fetch all registrations for this event
    List<Registration> registrations = registrationRepository.findByEventId(eventId);

    // Populate registration fields from student User object
    for (Registration reg : registrations) {
        User student = reg.getStudent();
        if (student != null) {
            reg.setName(student.getName());
            reg.setEmail(student.getEmail());
            reg.setPhone(student.getPhone());
            reg.setDepartment(student.getDepartment());
            reg.setYear(student.getYear());
            // Optional: default status if null
            reg.setStatus(reg.getStatus() != null ? reg.getStatus() : "Pending");
        }
    }

    // Pass data to Thymeleaf
    model.addAttribute("event", event);
    model.addAttribute("registrations", registrations);
    model.addAttribute("name", user.getName());
    model.addAttribute("role", user.getRole());

    return "faculty/registrations";
}

    // ================= Notifications =================
    @GetMapping("/faculty/notifications")
    public String showNotificationsPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user != null) {
            model.addAttribute("name", user.getName());
            model.addAttribute("role", user.getRole());
            model.addAttribute("currentPage", "notifications");
            List<Event> events = eventRepository.findByFaculty(user);
            model.addAttribute("events", events);
        }
        return "faculty/notifications";
    }

    @PostMapping("/faculty/notify-students")
    public String notifyStudents(@RequestParam("eventId") Long eventId,
                                 @RequestParam("message") String message,
                                 HttpSession session,
                                 Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";

        Event event = eventRepository.findById(eventId).orElse(null);

        if (event == null || !event.getFaculty().getId().equals(user.getId())) {
            model.addAttribute("error", "Invalid event or unauthorized.");
        } else {
            System.out.println("📢 Notification for Event: " + event.getName() + " → " + message);
            model.addAttribute("success", "Notification sent successfully!");
        }

        model.addAttribute("events", eventRepository.findByFaculty(user));
        model.addAttribute("name", user.getName());
        model.addAttribute("role", user.getRole());
        model.addAttribute("currentPage", "notifications");

        return "faculty/notifications";
    }
}
