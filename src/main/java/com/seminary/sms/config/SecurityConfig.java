package com.seminary.sms.config;

// ─────────────────────────────────────────────────────────────────────────────
// CONFIG / SUPPORT — SecurityConfig
// This is not part of any numbered layer. It is the central security
// configuration for the entire application, built on Spring Security.
//
// What it does:
//
//   1. HTTP Security Headers
//      Sets browser-level protections: blocks the page from being embedded in
//      iframes (clickjacking), enforces HTTPS (HSTS), and enables XSS filters.
//
//   2. Access Rules
//      Defines which URLs require login and which are public:
//        - /login.html, /css/**, /js/**, /images/**, /api/public/** → open to everyone
//        - Everything else → must be logged in
//
//   3. Form Login
//      Configures the login form at /login.html, processes credentials at POST /login.
//      On success → clears the brute-force counter and redirects to /index.html.
//      On failure → records the failed attempt; if locked, redirects with ?blocked=true.
//
//   4. Rate Limiting Filter
//      A custom filter that runs before Spring's own login filter.
//      If a username is blocked (too many failures), the request is rejected
//      immediately without even checking the password.
//
//   5. Logout
//      Handles GET /logout → invalidates the session, deletes the JSESSIONID cookie,
//      and redirects to /login.html?logout=true.
//
//   6. UserDetailsService (bean)
//      Tells Spring Security how to load a user from the database at login time.
//      Looks up the username in UserRepository and checks if the account is active.
//
//   7. PasswordEncoder (bean)
//      Uses BCrypt — a strong hashing algorithm — to hash and verify passwords.
//      Passwords are NEVER stored as plain text.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.User;
import com.seminary.sms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── HTTP Security Headers ─────────────────────────────────────────
            // SECURITY (A05): Prevent clickjacking, MIME sniffing, and XSS via headers
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(ct -> {})
                .xssProtection(xss -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            )

            // ── CSRF ──────────────────────────────────────────────────────────
            // Disabled — accepted risk for this deployment context.
            // This system uses plain static HTML (not Thymeleaf), which is incompatible
            // with Spring Security 6's lazy CSRF token loading. Properly enabling CSRF
            // would require migrating to Thymeleaf templates or a token-fetch endpoint.
            // Risk is low: local network deployment, single registrar, no public access.
            // Revisit if the system is ever deployed publicly or gains multiple admin users.
            .csrf(csrf -> csrf.disable())

            // ── Access Rules ──────────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Static files — always accessible (login page, CSS, JS)
                .requestMatchers(
                    "/login.html", "/login", "/apply.html", "/check-status.html",
                    "/css/**", "/js/**", "/images/**",
                    "/favicon.ico",
                    "/api/public/**"
                ).permitAll()
                // Registrar-only API endpoints
                .requestMatchers("/api/registrar/**").hasRole("Registrar")
                // Everything else needs login
                .anyRequest().authenticated()
            )

            // ── Login ─────────────────────────────────────────────────────────
            .formLogin(form -> form
                .loginPage("/login.html")         // show this page for login
                .loginProcessingUrl("/login")     // POST here with username/password
                // On success: clear rate-limit counter and redirect to main app
                .successHandler(loginSuccessHandler())
                // On failure: record attempt; redirect with blocked=true if account is locked
                .failureHandler(loginFailureHandler())
                .permitAll()
            )

            // ── Rate Limiting ─────────────────────────────────────────────────
            // SECURITY (A07): Reject login attempts for locked accounts before processing.
            // Tracks by username so only the specific account is locked, not the entire network.
            .addFilterBefore((request, response, chain) -> {
                jakarta.servlet.http.HttpServletRequest req = (jakarta.servlet.http.HttpServletRequest) request;
                if ("/login".equals(req.getServletPath()) && "POST".equalsIgnoreCase(req.getMethod())) {
                    String username = req.getParameter("username");
                    if (username != null && !username.isBlank() && loginAttemptService.isBlocked(username.trim())) {
                        ((jakarta.servlet.http.HttpServletResponse) response)
                            .sendRedirect("/login.html?blocked=true");
                        return;
                    }
                }
                chain.doFilter(request, response);
            }, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)

            // ── Logout ────────────────────────────────────────────────────────
            // Accept GET /logout so the link works without needing a CSRF token.
            // The lazy cookie issue in Spring Security 6 means the token is often
            // not set on static pages — GET logout is acceptable for a local system.
            .logout(logout -> logout
                .logoutRequestMatcher(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login.html?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    /**
     * After successful login, clear the rate-limit counter for this account
     * and redirect to the main app.
     */
    private AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> {
            loginAttemptService.loginSucceeded(authentication.getName());
            response.sendRedirect("/index.html");
        };
    }

    /**
     * After failed login, record the attempt for this account.
     * If the account is now locked, redirect with a blocked flag so the
     * login page can show an appropriate message.
     */
    private AuthenticationFailureHandler loginFailureHandler() {
        return (request, response, exception) -> {
            String username = request.getParameter("username");
            if (username != null && !username.isBlank()) {
                String trimmed = username.trim();
                loginAttemptService.loginFailed(trimmed);
                if (loginAttemptService.isBlocked(trimmed)) {
                    response.sendRedirect("/login.html?blocked=true");
                    return;
                }
                int remaining = loginAttemptService.getRemainingAttempts(trimmed);
                response.sendRedirect("/login.html?error=true&attempts=" + remaining);
                return;
            }
            response.sendRedirect("/login.html?error=true");
        };
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
            if (!user.getIsActive()) {
                throw new UsernameNotFoundException("Account is inactive.");
            }
            return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
