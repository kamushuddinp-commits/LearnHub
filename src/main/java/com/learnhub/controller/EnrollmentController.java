
package com.learnhub.controller;

import com.learnhub.service.EnrollmentService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students/{studentId}/enrollments")
public class EnrollmentController {

    private final EnrollmentService service;

    public EnrollmentController(EnrollmentService service) {
        this.service = service;
    }

    @PostMapping("/{courseId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> enroll(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {

        return service.enroll(studentId, courseId);
    }

    @GetMapping
    public List<Map<String, Object>> mine(
            @PathVariable Long studentId) {

        return service.forStudent(studentId);
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unenroll(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {

        service.unenroll(studentId, courseId);
    }
}
