
package com.learnhub.service;

import com.learnhub.model.Course;
import com.learnhub.model.Enrollment;
import com.learnhub.model.Student;
import com.learnhub.repository.EnrollmentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollments;
    private final StudentService studentService;
    private final CourseService courseService;

    public EnrollmentService(
            EnrollmentRepository enrollments,
            StudentService studentService,
            CourseService courseService) {

        this.enrollments = enrollments;
        this.studentService = studentService;
        this.courseService = courseService;
    }

    @Transactional
    public Map<String, Object> enroll(Long studentId, Long courseId) {

        Student student = studentService.getStudent(studentId);
        Course course = courseService.get(courseId);

        if (enrollments.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You are already enrolled in this course."
            );
        }

        Enrollment saved = enrollments.save(
                new Enrollment(student, course)
        );

        Long enrollmentId = Objects.requireNonNull(saved.getId());

        return Map.of(
                "message", "Enrollment successful",
                "enrollmentId", enrollmentId,
                "studentId", studentId,
                "courseId", courseId
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> forStudent(Long studentId) {

        studentService.getStudent(studentId);

        return enrollments.findByStudentId(studentId)
                .stream()
                .map(e -> {
                    Course c = e.getCourse();

                    return Map.<String, Object>of(
                            "enrollmentId", Objects.requireNonNull(e.getId()),
                            "courseId", Objects.requireNonNull(c.getId()),
                            "title", c.getTitle(),
                            "category", c.getCategory(),
                            "instructor", c.getInstructor(),
                            "durationWeeks", c.getDurationWeeks(),
                            "enrolledAt", e.getEnrolledAt().toString()
                    );
                })
                .toList();
    }

    @Transactional
    public void unenroll(Long studentId, Long courseId) {

        Enrollment enrollment = enrollments
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Enrollment not found."
                ));

        enrollments.delete(Objects.requireNonNull(enrollment));
    }
}
