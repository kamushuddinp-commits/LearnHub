package com.learnhub.dto;

import com.learnhub.model.Student;

public record StudentResponse(Long id, String name, String email) {
    public static StudentResponse from(Student s) {
        return new StudentResponse(s.getId(), s.getName(), s.getEmail());
    }
}
