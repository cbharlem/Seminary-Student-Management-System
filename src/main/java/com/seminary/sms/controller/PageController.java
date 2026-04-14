package com.seminary.sms.controller;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 2 — CONTROLLER (PageController)
// Handles browser page navigation — NOT an API controller.
//
// Unlike the other controllers in this project which are @RestController
// (returning JSON data), this one is a plain @Controller that returns
// redirect strings to point the browser at static HTML files.
//
// Why is this needed?
// When a user types a URL like "/dashboard" or "/students" directly into
// the browser, Spring needs to know what to send back. This controller
// redirects all app routes to index.html, where JavaScript takes over
// and shows the correct module based on the current URL.
//
// Redirects handled:
//   GET /login             → /login.html   (the login page)
//   GET / (and all app routes like /dashboard, /students, /grades, etc.)
//                          → /index.html   (the single-page app)
//
// This controller does NOT call any repository or service.
// It is purely a routing helper between the browser and the static files.
//
// LAYER 2 → LAYER 1: Redirects the browser to the correct static HTML file.
// ─────────────────────────────────────────────────────────────────────────────

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

    // LAYER 2 → LAYER 1: When a user navigates to /login, Spring redirects them to the static login.html file
    /** Login page — served directly as a static file */
    @GetMapping("/login")
    public String login() {
        return "redirect:/login.html";
    }

    // LAYER 2 → LAYER 1: When a user types any app URL directly into the browser (e.g. /students, /grades),
    //   Spring redirects them to index.html where JavaScript takes over and shows the correct page module.
    //   This is needed because index.html is a Single-Page App — all navigation happens inside it.
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
