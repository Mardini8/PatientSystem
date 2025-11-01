package com.PatientSystem.PatientSystem;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class HelloController {

    /**
     * En enkel GET-slutpunkt som returnerar en sträng.
     * Slutpunkten kommer att vara: http://localhost:8080/api/hello
     */
    @GetMapping("/hello")
    public String helloWorld() {
        return "Hello World from PatientSystem Spring Boot Backend!";
    }
}