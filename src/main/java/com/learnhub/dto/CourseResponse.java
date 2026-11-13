package com.learnhub.dto;

import com.learnhub.model.Course;

public record CourseResponse(Long id, String title, String category, String description,
                             String instructor, Integer durationWeeks, long enrolledCount) {
    public static CourseResponse from(Course c, long enrolledCount) {
        return new CourseResponse(c.getId(), c.getTitle(), c.getCategory(),
                c.getDescription(), c.getInstructor(), c.getDurationWeeks(), enrolledCount);
    }
}
