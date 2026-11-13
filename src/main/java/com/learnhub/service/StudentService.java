
package com.learnhub.service;

import com.learnhub.dto.*;
import com.learnhub.model.Student;
import com.learnhub.repository.StudentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
public class StudentService {

    private final StudentRepository students;
    private final BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder();

    public StudentService(StudentRepository students) {
        this.students = students;
    }

    public StudentResponse register(RegisterRequest request) {

        String email = request.email().trim().toLowerCase();

        if (students.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists."
            );
        }

        Student saved = students.save(new Student(
                request.name().trim(),
                email,
                encoder.encode(request.password())
        ));

        return StudentResponse.from(saved);
    }

    public StudentResponse login(LoginRequest request) {

        Student student = students.findByEmailIgnoreCase(
                request.email().trim()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password."
        ));

        if (!encoder.matches(
                request.password(),
                student.getPasswordHash()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password."
            );
        }

        return StudentResponse.from(student);
    }

    public Student getStudent(Long id) {

        Objects.requireNonNull(id, "Student ID cannot be null");

        return students.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Student not found."
                ));
    }
}
