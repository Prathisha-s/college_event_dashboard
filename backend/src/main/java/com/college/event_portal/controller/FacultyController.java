package com.college.event_portal.controller;

import com.college.event_portal.model.Event;
import com.college.event_portal.model.Notification;
import com.college.event_portal.model.Registration;
import com.college.event_portal.model.User;
import com.college.event_portal.model.Activity;
import com.college.event_portal.repository.ActivityRepository;
import com.college.event_portal.repository.EventRepository;
import com.college.event_portal.repository.NotificationRepository;
import com.college.event_portal.repository.RegistrationRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/faculty")
public class FacultyController {

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private RegistrationRepository registrationRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    // ================= Dashboard =================
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = getFacultyFromSession(session);
        if (user == null) return "redirect:/login";

        model.addAttribute("name", user.getName());
        model.addAttribute("role", user.getRole());
        model.addAttribute("currentPage", "dashboard");

        List<Event> allEvents = eventRepository.findByFaculty(user);

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<Event> activeEvents = allEvents.stream()
                .filter(e -> e.getDate() != null && (e.getDate().isAfter(today) ||
                        (e.getDate().isEqual(today) && e.getEndTime() != null && e.getEndTime().isAfter(now))))
                .collect(Collectors.toList());

        List<Event> expiredEvents = allEvents.stream()
                .filter(e -> e.getDate() == null || e.getDate().isBefore(today) ||
                        (e.getDate().isEqual(today) && e.getEndTime() != null && e.getEndTime().isBefore(now)))
                .collect(Collectors.toList());

        model.addAttribute("events", activeEvents);
        model.addAttribute("expiredEvents", expiredEvents);

        return "faculty/dashboard";
    }

    // ================= Manage Current Events =================
    @GetMapping("/manage-events")
    public String manageEvents(HttpSession session, Model model) {
        User user = getFacultyFromSession(session);
        if (user == null) return "redirect:/login";

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<Event> events = eventRepository.findByFaculty(user).stream()
                .filter(e -> e.getDate() != null && (e.getDate().isAfter(today) ||
                        (e.getDate().isEqual(today) && e.getEndTime() != null && e.getEndTime().isAfter(now))))
                .collect(Collectors.toList());

        model.addAttribute("events", events);
        model.addAttribute("name", user.getName());
        model.addAttribute("role", user.getRole());

        return "faculty/manage-events";
    }

    // ================= Manage Expired Events =================
    @GetMapping("/expired-events")
    public String expiredEvents(HttpSession session, Model model) {
        User user = getFacultyFromSession(session);
        if (user == null) return "redirect:/login";

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<Event> expiredEvents = eventRepository.findByFaculty(user).stream()
                .filter(e -> e.getDate() == null || e.getDate().isBefore(today) ||
                        (e.getDate().isEqual(today) && e.getEndTime() != null && e.getEndTime().isBefore(now)))
                .collect(Collectors.toList());

        model.addAttribute("events", expiredEvents);
        model.addAttribute("name", user.getName());
        model.addAttribute("role", user.getRole());

        return "faculty/expired-events";
    }

    // ================= Create Event =================
    @GetMapping("/create-event")
    public String createEvent(HttpSession session, Model model) {
        User user = getFacultyFromSession(session);
        if (user == null) return "redirect:/login";

        model.addAttribute("name", user.getName());
        model.addAttribute("role", user.getRole());
        model.addAttribute("currentPage", "create-event");
        model.addAttribute("event", new Event());

        return "faculty/create-event";
    }

    @PostMapping("/create-event")
    public String saveEvent(@ModelAttribute("event") Event event, HttpSession session) {
        User user = getFacultyFromSession(session);
        if (user == null) return "redirect:/login";

        event.setFaculty(user);

        // Convert dropdowns to LocalTime
        if (event.getStartHour() != null && event.getStartMinute() != null && event.getStartAmPm() != null) {
            int hour = event.getStartHour();
            int minute = Integer.parseInt(event.getStartMinute());

            if ("PM".equalsIgnoreCase(event.getStartAmPm()) && hour < 12) hour += 12;
            if ("AM".equalsIgnoreCase(event.getStartAmPm()) && hour == 12) hour = 0;

            event.setStartTime(LocalTime.of(hour, minute));
        }

        if (event.getEndHour() != null && event.getEndMinute() != null && event.getEndAmPm() != null) {
            int hour = event.getEndHour();
            int minute = Integer.parseInt(event.getEndMinute());

            if ("PM".equalsIgnoreCase(event.getEndAmPm()) && hour < 12) hour += 12;
            if ("AM".equalsIgnoreCase(event.getEndAmPm()) && hour == 12) hour = 0;

            event.setEndTime(LocalTime.of(hour, minute));
        }

        eventRepository.save(event);

        // ✅ Log activity
        activityRepository.save(new Activity(
                user,
                "Created Event: " + event.getName(),
                LocalDateTime.now()
        ));

        return "redirect:/faculty/dashboard";
    }

    // ================= View Registrations =================
    @GetMapping("/registrations/{eventId}")
    public String viewRegistrations(@PathVariable Long eventId, HttpSession session, Model model) {
        User user = getFacultyFromSession(session);
        if (user == null) return "redirect:/login";

        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null || !event.getFaculty().getId().equals(user.getId())) {
            return "redirect:/faculty/dashboard";
        }

        List<Registration> registrations = registrationRepository.findByEventId(eventId);
        registrations.forEach(reg -> {
            if (reg.getStatus() == null) reg.setStatus("Pending");
        });

        model.addAttribute("event", event);
        model.addAttribute("registrations", registrations);
        model.addAttribute("name", user.getName());
        model.addAttribute("role", user.getRole());

        return "faculty/registrations";
    }

    // ================= Approve / Reject Registration =================
    @PostMapping("/registrations/{id}/approve")
    @Transactional
    public String approveRegistration(@PathVariable Long id, @RequestParam(required = false) String comments) {
        Registration reg = registrationRepository.findById(id).orElseThrow();
        reg.setStatus("Approved");
        reg.setFacultyComments(comments);
        registrationRepository.save(reg);

        // Send notification
        Notification n = new Notification();
        n.setEvent(reg.getEvent());
        n.setStudent(reg.getStudent());
        n.setMessage("Your registration for " + reg.getEvent().getName() + " has been Approved.");
        n.setSentAt(LocalDateTime.now());
        notificationRepository.save(n);

        // ✅ Log activity
        activityRepository.save(new Activity(
                reg.getEvent().getFaculty(),
                "Approved registration of " + reg.getStudent().getName() +
                        " for event " + reg.getEvent().getName(),
                LocalDateTime.now()
        ));

        return "redirect:/faculty/registrations/" + reg.getEvent().getId();
    }

    @PostMapping("/registrations/{id}/reject")
    @Transactional
    public String rejectRegistration(@PathVariable Long id, @RequestParam(required = false) String comments) {
        Registration reg = registrationRepository.findById(id).orElseThrow();
        reg.setStatus("Rejected");
        reg.setFacultyComments(comments);
        registrationRepository.save(reg);

        // Send notification
        Notification n = new Notification();
        n.setEvent(reg.getEvent());
        n.setStudent(reg.getStudent());
        n.setMessage("Your registration for " + reg.getEvent().getName() + " has been Rejected.");
        n.setSentAt(LocalDateTime.now());
        notificationRepository.save(n);

        // ✅ Log activity
        activityRepository.save(new Activity(
                reg.getEvent().getFaculty(),
                "Rejected registration of " + reg.getStudent().getName() +
                        " for event " + reg.getEvent().getName(),
                LocalDateTime.now()
        ));

        return "redirect:/faculty/registrations/" + reg.getEvent().getId();
    }

    // ================= Notifications =================
    @GetMapping("/notifications")
    public String showNotificationForm(HttpSession session, Model model) {
        User user = getFacultyFromSession(session);
        if (user == null) return "redirect:/login";

        // 🔽 Filter only active/upcoming events
        List<Event> activeEvents = eventRepository.findByFaculty(user).stream()
                .filter(e -> e.getDate() != null && !e.getDate().isBefore(LocalDate.now()))
                .collect(Collectors.toList());

        model.addAttribute("events", activeEvents);
        model.addAttribute("name", user.getName());
        model.addAttribute("role", user.getRole());

        return "faculty/notifications";
    }

    @PostMapping("/notify-students")
    public String notifyStudents(@RequestParam("eventId") Long eventId,
                                 @RequestParam("message") String message,
                                 HttpSession session,
                                 Model model) {
        User user = getFacultyFromSession(session);
        if (user == null) return "redirect:/login";

        Event event = eventRepository.findById(eventId).orElse(null);

        if (event != null && event.getFaculty().getId().equals(user.getId())) {
            List<Registration> registrations = registrationRepository.findByEventId(eventId);
            for (Registration reg : registrations) {
                Notification n = new Notification();
                n.setEvent(event);
                n.setStudent(reg.getStudent());
                n.setMessage(message);
                n.setSentAt(LocalDateTime.now());
                notificationRepository.save(n);
            }

            // ✅ Log activity
            activityRepository.save(new Activity(
                    user,
                    "Sent notification to students for event: " + event.getName(),
                    LocalDateTime.now()
            ));

            model.addAttribute("success", "Notification sent successfully!");
        } else {
            model.addAttribute("error", "Event not found or you are not authorized.");
        }

        // Re-populate events for the select dropdown
        List<Event> activeEvents = eventRepository.findByFaculty(user).stream()
                .filter(e -> e.getDate() != null && !e.getDate().isBefore(LocalDate.now()))
                .collect(Collectors.toList());

        model.addAttribute("events", activeEvents);
        model.addAttribute("name", user.getName());
        model.addAttribute("role", user.getRole());

        // Keep the selected event and message in the form
        model.addAttribute("selectedEventId", eventId);
        model.addAttribute("messageText", message);

        return "faculty/notifications";
    }

    // ================= Helper: Get faculty from session =================
    private User getFacultyFromSession(HttpSession session) {
        Object obj = session.getAttribute("currentUser");
        if (obj instanceof User user && "ROLE_FACULTY".equals(user.getRole())) {
            return user;
        }
        return null;
    }
}
