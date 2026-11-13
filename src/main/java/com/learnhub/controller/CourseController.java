package com.learnhub.controller;

import com.learnhub.dto.CourseResponse;
import com.learnhub.service.CourseService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
    private final CourseService service;
    public CourseController(CourseService service) { this.service = service; }

    @GetMapping
    public List<CourseResponse> all() { return service.all(); }
}
