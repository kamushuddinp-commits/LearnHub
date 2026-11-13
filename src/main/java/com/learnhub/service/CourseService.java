
package com.learnhub.service;

import com.learnhub.dto.CourseResponse;
import com.learnhub.model.Course;
import com.learnhub.repository.CourseRepository;
import com.learnhub.repository.EnrollmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class CourseService {

    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;

    public CourseService(CourseRepository courses,
                         EnrollmentRepository enrollments) {
        this.courses = courses;
        this.enrollments = enrollments;
    }

    public List<CourseResponse> all() {
        return courses.findAll().stream()
                .map(c -> CourseResponse.from(
                        c,
                        enrollments.countByCourseId(
                                Objects.requireNonNull(c.getId())
                        )
                ))
                .toList();
    }

    public Course get(Long id) {
        Objects.requireNonNull(id, "Course ID cannot be null");

        return courses.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Course not found."
                ));
    }
}
