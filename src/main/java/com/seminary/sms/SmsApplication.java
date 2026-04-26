package com.seminary.sms;

// ─────────────────────────────────────────────────────────────────────────────
// ENTRY POINT — SmsApplication
// This is the starting point of the entire Spring Boot application.
// When you run this file, it launches the embedded web server (Tomcat),
// scans all classes in the com.seminary.sms package for Spring annotations
// (@Entity, @Repository, @Service, @RestController, @Configuration, etc.),
// and wires everything together automatically.
//
// @SpringBootApplication is a shortcut for three annotations combined:
//   @Configuration     → marks this as a source of Spring bean definitions
//   @EnableAutoConfiguration → tells Spring Boot to auto-configure itself
//                              based on the libraries on the classpath
//   @ComponentScan     → scans this package and all sub-packages for Spring components
//
// Think of this file as the "power button" — it does not contain any business
// logic itself, but it is what makes the whole system start running.
//
// The application listens on the port defined in application.properties (default: 8080).
// ─────────────────────────────────────────────────────────────────────────────

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmsApplication.class, args);
    }
}
