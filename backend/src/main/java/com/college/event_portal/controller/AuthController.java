package com.college.event_portal.controller;

import com.college.event_portal.model.User;
import com.college.event_portal.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ---------------- Login Page ----------------
    @GetMapping("/login")
    public String showLoginForm(Model model,
                                @RequestParam(required = false) String error,
                                @RequestParam(required = false) String logout,
                                @RequestParam(required = false) String success) {
        if (error != null) model.addAttribute("error", "Invalid email or password");
        if (logout != null) model.addAttribute("success", "Logged out successfully");
        if (success != null) model.addAttribute("success", success);
        return "login";
    }

    // ---------------- Register Page ----------------
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    // ---------------- Handle Registration ----------------
    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, Model model) {
        // Lowercase email for uniqueness
        user.setEmail(user.getEmail().toLowerCase());

        // Check if email already exists
        if (userRepository.findByEmailIgnoreCase(user.getEmail()).isPresent()) {
            model.addAttribute("error", "Email already registered");
            return "register";
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Ensure role is correct; use default ROLE_STUDENT if not set
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("ROLE_STUDENT");
        }
        // Otherwise, keep the role exactly as selected from the form (ROLE_STUDENT / ROLE_FACULTY / ROLE_ADMIN)

        // Save user
        userRepository.save(user);

        // Redirect to login with success message
        return "redirect:/login?success=Registration successful!";
    }

    // ---------------- Welcome Page (after login) ----------------
    @GetMapping("/welcome")
    public String welcome(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user != null) {
            model.addAttribute("name", user.getName());
            model.addAttribute("role", user.getRole());
            return "welcome";
        }
        return "redirect:/login";
    }
}
