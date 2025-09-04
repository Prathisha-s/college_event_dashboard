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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    // ---------------- Helper: Get student from session ----------------
    private User getStudentFromSession(HttpSession session) {
        User student = (User) session.getAttribute("currentUser");
        if (student == null || !"ROLE_STUDENT".equals(student.getRole())) {
            return null;
        }
        return student;
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
            events = events.stream()
                    .filter(e -> e.getName().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (category != null && !category.isEmpty()) {
            events = events.stream()
                    .filter(e -> e.getCategory() != null && category.equalsIgnoreCase(e.getCategory()))
                    .collect(Collectors.toList());
        }

        List<Long> registeredEventIds = registrationRepository.findByStudentId(student.getId())
                .stream()
                .map(r -> r.getEvent() != null ? r.getEvent().getId() : null)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        model.addAttribute("events", events);
        model.addAttribute("registeredEventIds", registeredEventIds);
        model.addAttribute("name", student.getName());
        model.addAttribute("role", student.getRole());

        return "student/dashboard";
    }

    // ---------------- Show Registration Form ----------------
    @GetMapping("/register-form/{eventId}")
    public String showRegistrationForm(@PathVariable Long eventId, HttpSession session, Model model) {

        User student = getStudentFromSession(session);
        if (student == null) return "redirect:/login";

        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            session.setAttribute("errorMessage", "Event not found");
            return "redirect:/student/dashboard";
        }

        model.addAttribute("event", event);
        model.addAttribute("student", student);
        return "student/register-form";
    }

    // ---------------- Submit Registration ----------------
    @PostMapping("/register/{eventId}")
    public String submitRegistration(@PathVariable Long eventId,
                                     @RequestParam String name,
                                     @RequestParam String regNo,
                                     @RequestParam String phone,
                                     @RequestParam String department,
                                     @RequestParam String year,
                                     @RequestParam String email,
                                     HttpSession session) {

        User student = getStudentFromSession(session);
        if (student == null) return "redirect:/login";

        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return "redirect:/student/dashboard";

        boolean alreadyRegistered = registrationRepository.findByEventId(eventId).stream()
                .anyMatch(r -> r.getStudent() != null && r.getStudent().getId().equals(student.getId()));

        if (!alreadyRegistered) {
            Registration reg = new Registration();
            reg.setEvent(event);
            reg.setStudent(student);
            reg.setStatus("Pending");
            reg.setRegistrationTime(LocalDateTime.now());

            reg.setName(name);
            reg.setRegNo(regNo);
            reg.setPhone(phone);
            reg.setDepartment(department);
            reg.setYear(year);
            reg.setEmail(email);

            registrationRepository.save(reg);
        }

        return "redirect:/student/my-registrations";
    }

    // ---------------- View My Registrations ----------------
    @GetMapping("/my-registrations")
    public String myRegistrations(HttpSession session, Model model) {

        User student = getStudentFromSession(session);
        if (student == null) return "redirect:/login";

        List<Registration> registrations = registrationRepository.findByStudentId(student.getId());

        // Set canCancel for each registration (within 5 hours)
        registrations.forEach(r -> {
            boolean canCancel = r.getRegistrationTime() != null
                    && Duration.between(r.getRegistrationTime(), LocalDateTime.now()).toHours() <= 5;
            r.setCanCancel(canCancel);
        });

        model.addAttribute("registrations", registrations);
        model.addAttribute("name", student.getName());
        model.addAttribute("role", student.getRole());

        return "student/my-registrations";
    }

    // ---------------- Cancel Registration ----------------
    @PostMapping("/cancel/{registrationId}")
    public String cancelRegistration(@PathVariable Long registrationId, HttpSession session) {

        User student = getStudentFromSession(session);
        if (student == null) return "redirect:/login";

        Registration reg = registrationRepository.findById(registrationId).orElse(null);
        if (reg == null || reg.getStudent() == null || !reg.getStudent().getId().equals(student.getId())) {
            return "redirect:/student/my-registrations";
        }

        if (Duration.between(reg.getRegistrationTime(), LocalDateTime.now()).toHours() <= 5) {
            registrationRepository.delete(reg);
        }

        return "redirect:/student/my-registrations";
    }
}
