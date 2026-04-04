package com.seminary.sms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * PageController — routes browser page requests to static HTML files.
 *
 * The actual HTML/CSS/JS frontend lives in:
 *   src/main/resources/static/
 *       login.html   ← login page
 *       index.html   ← main SPA (all modules)
 *       css/style.css
 *       js/api.js
 *       js/app.js
 *
 * Spring Boot serves the static/ folder automatically.
 * This controller just handles redirects so that typing
 * any app URL in the browser lands on the right page.
 */
@Controller
public class PageController {

    /** Login page — served directly as a static file */
    @GetMapping("/login")
    public String login() {
        return "redirect:/login.html";
    }

    /**
     * All app pages go to index.html (the SPA).
     * JavaScript inside index.html handles which module to show
     * based on the current URL path.
     */
    @GetMapping({"/", "/dashboard", "/applicants", "/enrollment",
                 "/students", "/alumni", "/curriculum", "/schedule",
                 "/grades", "/reports", "/backup", "/sections",
                 "/instructors", "/rooms", "/users", "/school-years",
                 "/my-grades", "/my-schedule", "/my-profile"})
    public String app() {
        return "redirect:/index.html";
    }
}
