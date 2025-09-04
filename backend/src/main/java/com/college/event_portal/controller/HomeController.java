package com.college.event_portal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Keep a simple welcome page for non-logged users
    @GetMapping("/home")
    public String home() {
        return "index"; // loads src/main/resources/templates/index.html
    }
}
