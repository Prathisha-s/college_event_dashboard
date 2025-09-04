package com.college.event_portal.controller;

import com.college.event_portal.model.User;
import com.college.event_portal.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/postLogin")
    public String postLogin(@AuthenticationPrincipal UserDetails userDetails, HttpSession session) {
        User user = userRepository.findByEmailIgnoreCase(userDetails.getUsername()).orElse(null);

        if (user != null) {
            session.setAttribute("currentUser", user);
            session.setAttribute("name", user.getName());
            session.setAttribute("role", user.getRole());

            switch (user.getRole()) {
                case "ROLE_FACULTY": return "redirect:/faculty/dashboard";
                case "ROLE_STUDENT": return "redirect:/student/dashboard";
                case "ROLE_ADMIN": return "redirect:/admin/dashboard";
            }
        }
        return "redirect:/login";
    }
}
