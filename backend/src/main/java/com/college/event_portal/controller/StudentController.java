package com.college.event_portal.controller;

import com.college.event_portal.model.Notification;
import com.college.event_portal.model.Event;
import com.college.event_portal.model.Registration;
import com.college.event_portal.model.User;
import com.college.event_portal.model.Activity;
import com.college.event_portal.repository.EventRepository;
import com.college.event_portal.repository.RegistrationRepository;
import com.college.event_portal.repository.NotificationRepository;
import com.college.event_portal.repository.ActivityRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ActivityRepository activityRepository;

    // ---------------- Helper ----------------
    private User getStudentFromSession(HttpSession session) {
        Object obj = session.getAttribute("currentUser");
        if (obj instanceof User student && "ROLE_STUDENT".equals(student.getRole())) {
            return student;
        }
        return null;
    }

    // ---------------- Dashboard ----------------
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session,
                            @RequestParam(required = false) String search,
                            @RequestParam(required = false) String category,
                            Model model) {

        User student = getStudentFromSession(session);
        if (student == null) return "redirect:/login";

        List<Event> events = eventRepository.findAll();

        if (search != null && !search.isEmpty()) {
            String keyword = search.toLowerCase();
            events = events.stream()
                    .filter(e -> e.getName() != null && e.getName().toLowerCase().contains(keyword))
                    .collect(Collectors.toList());
        }

        if (category != null && !category.isEmpty()) {
            events = events.stream()
                    .filter(e -> e.getCategory() != null && category.equalsIgnoreCase(e.getCategory()))
                    .collect(Collectors.toList());
        }

        List<Long> registeredEventIds = registrationRepository.findByStudentIdWithEvent(student.getId())
                .stream()
                .map(r -> r.getEvent() != null ? r.getEvent().getId() : -1)
                .collect(Collectors.toList());

        model.addAttribute("events", events);
        model.addAttribute("registeredEventIds", registeredEventIds);
        model.addAttribute("name", student.getName());
        model.addAttribute("role", student.getRole());
        model.addAttribute("search", search);
        model.addAttribute("category", category);

        return "student/dashboard";
    }

    // ---------------- Registration Form ----------------
    @GetMapping("/register-form/{eventId}")
    public String showRegisterForm(@PathVariable Long eventId,
                                   Model model,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {

        User student = getStudentFromSession(session);
        if (student == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Please login first.");
            return "redirect:/login";
        }

        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Event not found.");
            return "redirect:/student/dashboard";
        }

        boolean alreadyRegistered = registrationRepository.findByEventId(eventId).stream()
                .anyMatch(r -> r.getStudent() != null && r.getStudent().getId().equals(student.getId()));

        if (alreadyRegistered) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ You are already registered for this event.");
            return "redirect:/student/my-registrations";
        }

        model.addAttribute("registration", new Registration());
        model.addAttribute("event", event);
        model.addAttribute("student", student);

        return "student/register-form";
    }

    // ---------------- Submit Registration ----------------
    @PostMapping("/register/{eventId}")
    @Transactional
    public String submitRegistration(@PathVariable Long eventId,
                                     @RequestParam String name,
                                     @RequestParam String regNo,
                                     @RequestParam String phone,
                                     @RequestParam String department,
                                     @RequestParam String year,
                                     @RequestParam String email,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {

        User student = getStudentFromSession(session);
        if (student == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Please login first.");
            return "redirect:/login";
        }

        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Event not found.");
            return "redirect:/student/dashboard";
        }

        boolean alreadyRegistered = registrationRepository.findByEventId(eventId).stream()
                .anyMatch(r -> r.getStudent() != null && r.getStudent().getId().equals(student.getId()));

        if (alreadyRegistered) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ You are already registered for this event.");
            return "redirect:/student/my-registrations";
        }

        Registration registration = new Registration();
        registration.setEvent(event);
        registration.setStudent(student);
        registration.setName(name);
        registration.setRegNo(regNo);
        registration.setPhone(phone);
        registration.setDepartment(department);
        registration.setYear(year);
        registration.setEmail(email);
        registration.setStatus("Pending");
        registration.setRegistrationTime(LocalDateTime.now());

        registrationRepository.save(registration);

        // ---------- Activity log ----------
        Activity act = new Activity();
        act.setAction("Registered for Event");
        act.setDetails("Student registered for: " + event.getName());
        act.setUser(student);
        act.setEvent(event);
        act.setTimestamp(LocalDateTime.now());
        activityRepository.save(act);

        redirectAttributes.addFlashAttribute("successMessage", "✅ Event registered successfully!");
        return "redirect:/student/my-registrations";
    }

    // ---------------- Redirect ----------------
    @GetMapping("/register/{eventId}")
    public String redirectToRegisterForm(@PathVariable Long eventId) {
        return "redirect:/student/register-form/" + eventId;
    }

    // ---------------- My Registrations ----------------
    @GetMapping("/my-registrations")
    public String myRegistrations(HttpSession session, Model model) {
        User student = getStudentFromSession(session);
        if (student == null) return "redirect:/login";

        List<Registration> registrations = registrationRepository.findByStudentIdWithEvent(student.getId());
        if (registrations == null) registrations = Collections.emptyList();

        // Check cancellation eligibility
        registrations.forEach(r -> {
            boolean canCancel = false;
            if (r.getRegistrationTime() != null) {
                canCancel = Duration.between(r.getRegistrationTime(), LocalDateTime.now()).toHours() < 5;
            }
            r.setCanCancel(canCancel);
        });

        model.addAttribute("registrations", registrations);
        model.addAttribute("name", student.getName());
        model.addAttribute("role", student.getRole());

        return "student/my-registrations";
    }

    // ---------------- Cancel Registration ----------------
    @PostMapping("/cancel/{registrationId}")
    @Transactional
    public String cancelRegistration(@PathVariable Long registrationId,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {

        User student = getStudentFromSession(session);
        if (student == null) return "redirect:/login";

        Registration reg = registrationRepository.findById(registrationId).orElse(null);
        if (reg == null || reg.getStudent() == null || !reg.getStudent().getId().equals(student.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠️ Registration not found or unauthorized.");
            return "redirect:/student/my-registrations";
        }

        if (reg.getRegistrationTime() != null &&
                Duration.between(reg.getRegistrationTime(), LocalDateTime.now()).toHours() < 5) {

            registrationRepository.delete(reg);

            // ---------- Activity log ----------
            Activity act = new Activity();
            act.setAction("Canceled Registration");
            act.setDetails("Student canceled registration for: " + reg.getEvent().getName());
            act.setUser(student);
            act.setEvent(reg.getEvent());
            act.setTimestamp(LocalDateTime.now());
            activityRepository.save(act);

            redirectAttributes.addFlashAttribute("successMessage", "✅ Registration canceled successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "⏳ Cancellation period expired.");
        }

        return "redirect:/student/my-registrations";
    }

    // ---------------- Notifications ----------------
    @GetMapping("/notifications")
    public String notifications(HttpSession session, Model model) {
        User student = getStudentFromSession(session);
        if (student == null) return "redirect:/login";

        List<Notification> notifications = notificationRepository.findByStudentWithEvent(student);

        model.addAttribute("notifications", notifications);
        model.addAttribute("name", student.getName());
        model.addAttribute("role", student.getRole());

        return "student/notifications";
    }
}
