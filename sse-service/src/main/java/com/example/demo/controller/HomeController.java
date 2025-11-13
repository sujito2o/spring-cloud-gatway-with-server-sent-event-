package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for serving the main UI page
 */
@Controller
public class HomeController {

    /**
     * Serves the main dashboard page
     */
    @GetMapping("/")
    public String home() {
        return "index";
    }
}