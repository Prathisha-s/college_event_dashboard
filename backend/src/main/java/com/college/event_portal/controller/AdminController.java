package com.college.event_portal.controller;

import com.college.event_portal.model.*;
import com.college.event_portal.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private EventRepository eventRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RegistrationRepository registrationRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ActivityRepository activityRepository;

    // ---------------- Helper: Get admin from session ----------------
    private User getAdminFromSession(HttpSession session) {
        Object obj = session.getAttribute("currentUser");
        if (obj instanceof User user && "ROLE_ADMIN".equals(user.getRole())) {
            return user;
        }
        return null;
    }

    private LocalTime convertToLocalTime(Integer hour, String minute, String amPm) {
        if (hour == null || minute == null || amPm == null) return null;
        int h = hour;
        int m = Integer.parseInt(minute);
        if ("PM".equals(amPm) && h < 12) h += 12;
        if ("AM".equals(amPm) && h == 12) h = 0;
        return LocalTime.of(h, m);
    }

    // ---------------- Admin Dashboard ----------------
   @GetMapping("/dashboard")
public String adminDashboard(HttpSession session, Model model) {
    User admin = getAdminFromSession(session);
    if (admin == null) return "redirect:/login";

    // Fetch all events and registrations
    List<Event> allEvents = Optional.ofNullable(eventRepository.findAll()).orElse(List.of());
    List<Registration> allRegs = Optional.ofNullable(registrationRepository.findAll()).orElse(List.of());

    // Pending registrations count
    long pendingRegs = allRegs.stream()
            .filter(r -> r != null && "Pending".equalsIgnoreCase(r.getStatus()))
            .count();

    // Monthly registrations (Jan-Dec)
    Map<Integer, Long> monthlyMap = allRegs.stream()
            .filter(r -> r != null && r.getRegistrationTime() != null)
            .collect(Collectors.groupingBy(
                    r -> r.getRegistrationTime().getMonthValue(),
                    Collectors.counting()
            ));
    List<Long> monthlyRegistrations = new ArrayList<>();
    for (int m = 1; m <= 12; m++) {
        monthlyRegistrations.add(monthlyMap.getOrDefault(m, 0L));
    }

    // Event status counts
    LocalDate today = LocalDate.now();
    long expiredEvents = allEvents.stream()
            .filter(e -> e != null && e.getDate() != null && e.getDate().isBefore(today))
            .count();
    long activeEvents = allEvents.stream()
            .filter(e -> e != null && e.getDate() != null && e.getDate().isEqual(today))
            .count();
    long upcomingEvents = allEvents.stream()
            .filter(e -> e != null && e.getDate() != null && e.getDate().isAfter(today))
            .count();

    // Recent Activities (Top 5)
    List<Activity> recentActivities = Optional.ofNullable(activityRepository.findTop5ByOrderByTimestampDesc())
    .orElse(List.of());

if (recentActivities.isEmpty()) {
    Activity placeholder = new Activity();
    placeholder.setAction("No recent activity");
    placeholder.setDetails("");
    placeholder.setTimestamp(LocalDateTime.now());
    recentActivities = List.of(placeholder);
} else {
    recentActivities.forEach(act -> {
        if (act.getAction() == null) act.setAction("Action not defined");
        if (act.getDetails() == null) act.setDetails("");
        if (act.getTimestamp() == null) act.setTimestamp(LocalDateTime.now());
    });
}

    // Add attributes for Thymeleaf
    model.addAttribute("name", admin.getName() != null ? admin.getName() : "Admin");
    model.addAttribute("totalEvents", allEvents.size());
    model.addAttribute("totalRegistrations", allRegs.size());
    model.addAttribute("pendingRegistrations", pendingRegs);
    model.addAttribute("activeEvents", activeEvents);
    model.addAttribute("expiredEvents", expiredEvents);
    model.addAttribute("upcomingEvents", upcomingEvents);
    model.addAttribute("monthlyRegistrations", monthlyRegistrations);
    model.addAttribute("recentActivities", recentActivities);

    return "admin/dashboard";
}
    // ---------------- Manage Events ----------------
    @GetMapping("/events")
    public String manageEvents(HttpSession session, Model model) {
        User admin = getAdminFromSession(session);
        if (admin == null) return "redirect:/login";

        List<Event> events = eventRepository.findAll();

        events.forEach(e -> {
            int count = registrationRepository.findByEventId(e.getId()).size();
            e.setRegistrationCount(count);
        });

        model.addAttribute("events", events);
        model.addAttribute("name", admin.getName());
        return "admin/events-list";
    }

    @GetMapping("/events/create")
    public String showCreateEventForm(HttpSession session, Model model) {
        User admin = getAdminFromSession(session);
        if (admin == null) return "redirect:/login";

        model.addAttribute("event", new Event());
        model.addAttribute("name", admin.getName());
        return "admin/add-event";
    }

    @PostMapping("/events/save")
    @Transactional
    public String saveEvent(@ModelAttribute Event event, HttpSession session) {
        User admin = getAdminFromSession(session);
        if (admin == null) return "redirect:/login";

        eventRepository.save(event);

        // Log activity
        Activity act = new Activity();
        act.setAction("Created Event");
        act.setDetails("Admin created event " + event.getName());
        act.setUser(admin);
        act.setEvent(event);
        act.setTimestamp(LocalDateTime.now());
        activityRepository.save(act);

        return "redirect:/admin/events";
    }

    @GetMapping("/events/edit/{id}")
    public String editEventForm(@PathVariable Long id, HttpSession session, Model model) {
        User admin = getAdminFromSession(session);
        if (admin == null) return "redirect:/login";

        Optional<Event> optEvent = eventRepository.findById(id);
        if (optEvent.isEmpty()) return "redirect:/admin/events";

        Event e = optEvent.get();

        if (e.getStartTime() != null) {
            int startHour24 = e.getStartTime().getHour();
            e.setStartAmPm(startHour24 >= 12 ? "PM" : "AM");
            e.setStartHour(startHour24 % 12 == 0 ? 12 : startHour24 % 12);
            e.setStartMinute(String.format("%02d", e.getStartTime().getMinute()));
        }
        if (e.getEndTime() != null) {
            int endHour24 = e.getEndTime().getHour();
            e.setEndAmPm(endHour24 >= 12 ? "PM" : "AM");
            e.setEndHour(endHour24 % 12 == 0 ? 12 : endHour24 % 12);
            e.setEndMinute(String.format("%02d", e.getEndTime().getMinute()));
        }

        model.addAttribute("event", e);
        model.addAttribute("name", admin.getName());
        return "admin/edit-event";
    }

    @PostMapping("/events/edit/{id}")
    @Transactional
    public String updateEvent(@PathVariable Long id, @ModelAttribute Event updatedEvent, HttpSession session) {
        User admin = getAdminFromSession(session);
        if (admin == null) return "redirect:/login";

        eventRepository.findById(id).ifPresent(event -> {
            event.setName(updatedEvent.getName());
            event.setDescription(updatedEvent.getDescription());
            event.setVenue(updatedEvent.getVenue());
            event.setDate(updatedEvent.getDate());
            event.setStartTime(convertToLocalTime(updatedEvent.getStartHour(),
                    updatedEvent.getStartMinute(), updatedEvent.getStartAmPm()));
            event.setEndTime(convertToLocalTime(updatedEvent.getEndHour(),
                    updatedEvent.getEndMinute(), updatedEvent.getEndAmPm()));
            eventRepository.save(event);

            // Log activity
            Activity act = new Activity();
            act.setAction("Updated Event");
            act.setDetails("Admin updated event " + event.getName());
            act.setUser(admin);
            act.setEvent(event);
            act.setTimestamp(LocalDateTime.now());
            activityRepository.save(act);
        });

        return "redirect:/admin/events";
    }

    @PostMapping("/events/delete/{id}")
    @Transactional
    public String deleteEvent(@PathVariable Long id, HttpSession session) {
        User admin = getAdminFromSession(session);
        if (admin == null) return "redirect:/login";

        eventRepository.findById(id).ifPresent(event -> {
            eventRepository.delete(event);

            // Log activity
            Activity act = new Activity();
            act.setAction("Deleted Event");
            act.setDetails("Admin deleted event " + event.getName());
            act.setUser(admin);
            act.setEvent(event);
            act.setTimestamp(LocalDateTime.now());
            activityRepository.save(act);
        });

        return "redirect:/admin/events";
    }

    @GetMapping("/events/view/{id}")
    public String viewEvent(@PathVariable Long id, HttpSession session, Model model) {
        User admin = getAdminFromSession(session);
        if (admin == null) return "redirect:/login";

        Optional<Event> eventOpt = eventRepository.findById(id);
        if (eventOpt.isEmpty()) return "redirect:/admin/events";

        model.addAttribute("event", eventOpt.get());
        model.addAttribute("name", admin.getName());
        return "admin/view-event";
    }

    // ---------------- Manage Users ----------------
    @GetMapping("/users")
    public String users(HttpSession session, Model model) {
        User admin = getAdminFromSession(session);
        if (admin == null) return "redirect:/login";

        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("name", admin.getName());
        return "admin/manage-users";
    }

    @PostMapping("/users/change-role/{userId}")
    @Transactional
    public String changeUserRole(@PathVariable Long userId, @RequestParam String role,
                                 RedirectAttributes redirectAttributes, HttpSession session) {
        User admin = getAdminFromSession(session);
        if (admin == null) return "redirect:/login";

        userRepository.findById(userId).ifPresent(user -> {
            user.setRole(role);
            userRepository.save(user);

            // Log activity
            Activity act = new Activity();
            act.setAction("Changed Role");
            act.setDetails("Admin changed role of " + user.getName() + " to " + role);
            act.setUser(admin);
            act.setTimestamp(LocalDateTime.now());
            activityRepository.save(act);

            redirectAttributes.addFlashAttribute("successMessage", "✅ Role updated successfully!");
        });
        return "redirect:/admin/users";
    }

    // ---------------- Registrations ----------------
    @GetMapping("/registrations")
    public String registrations(HttpSession session, Model model) {
        User admin = getAdminFromSession(session);
        if (admin == null) return "redirect:/login";

        model.addAttribute("registrations", registrationRepository.findAll());
        model.addAttribute("name", admin.getName());
        return "admin/registrations";
    }

    // Approve registration
    @PostMapping("/registrations/approve/{id}")
    @Transactional
    public String approveRegistration(@PathVariable Long id, HttpSession session) {
        User admin = getAdminFromSession(session);
        if (admin == null) return "redirect:/login";

        registrationRepository.findById(id).ifPresent(reg -> {
            reg.setStatus("Approved");
            registrationRepository.save(reg);

            // Log activity
            Activity act = new Activity();
            act.setAction("Approved Registration");
            act.setDetails("Admin approved registration of " + reg.getStudent().getName() +
                    " for event " + reg.getEvent().getName());
            act.setUser(admin);
            act.setEvent(reg.getEvent());
            act.setTimestamp(LocalDateTime.now());
            activityRepository.save(act);
        });

        return "redirect:/admin/registrations";
    }

    // Reject registration
    @PostMapping("/registrations/reject/{id}")
    @Transactional
    public String rejectRegistration(@PathVariable Long id, HttpSession session) {
        User admin = getAdminFromSession(session);
        if (admin == null) return "redirect:/login";

        registrationRepository.findById(id).ifPresent(reg -> {
            reg.setStatus("Rejected");
            registrationRepository.save(reg);

            // Log activity
            Activity act = new Activity();
            act.setAction("Rejected Registration");
            act.setDetails("Admin rejected registration of " + reg.getStudent().getName() +
                    " for event " + reg.getEvent().getName());
            act.setUser(admin);
            act.setEvent(reg.getEvent());
            act.setTimestamp(LocalDateTime.now());
            activityRepository.save(act);
        });

        return "redirect:/admin/registrations";
    }

    // ---------------- Notifications ----------------
    @GetMapping("/notifications")
    public String notifications(HttpSession session, Model model) {
        User admin = getAdminFromSession(session);
        if (admin == null) return "redirect:/login";

        model.addAttribute("events", eventRepository.findAll());
        model.addAttribute("name", admin.getName());
        return "admin/send-notifications";
    }

    @PostMapping("/notifications/send")
    @Transactional
    public String sendNotification(@RequestParam Long eventId, @RequestParam String message,
                                   RedirectAttributes redirectAttributes, HttpSession session) {
        User admin = getAdminFromSession(session);
        if (admin == null) return "redirect:/login";

        eventRepository.findById(eventId).ifPresent(event -> {
            registrationRepository.findByEventId(eventId).forEach(reg -> {
                Notification notification = new Notification();
                notification.setStudent(reg.getStudent());
                notification.setMessage(message);
                notification.setSentAt(LocalDateTime.now());
                notificationRepository.save(notification);
            });

            // Log activity
            Activity act = new Activity();
            act.setAction("Sent Notification");
            act.setDetails("Admin sent notification for event " + event.getName() + ": " + message);
            act.setUser(admin);
            act.setEvent(event);
            act.setTimestamp(LocalDateTime.now());
            activityRepository.save(act);

            redirectAttributes.addFlashAttribute("successMessage", "✅ Notification sent successfully!");
        });

        return "redirect:/admin/notifications";
    }

    // ---------------- Reports ----------------
    @GetMapping("/reports")
    public String reports(HttpSession session, Model model) {
        User admin = getAdminFromSession(session);
        if (admin == null) return "redirect:/login";

        List<Map<String, Object>> reports = eventRepository.findAll().stream().map(e -> {
            List<Registration> regs = registrationRepository.findByEventId(e.getId());
            Map<String, Object> r = new HashMap<>();
            r.put("eventName", e.getName());
            r.put("total", regs.size());
            r.put("approved", regs.stream().filter(reg -> "Approved".equals(reg.getStatus())).count());
            r.put("pending", regs.stream().filter(reg -> "Pending".equals(reg.getStatus())).count());
            r.put("rejected", regs.stream().filter(reg -> "Rejected".equals(reg.getStatus())).count());
            return r;
        }).collect(Collectors.toList());

        model.addAttribute("reports", reports);
        model.addAttribute("name", admin.getName());
        return "admin/event-reports";
    }
}
