package com.learnhub.repository;

import com.learnhub.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    boolean existsByTitleIgnoreCase(String title);
}
