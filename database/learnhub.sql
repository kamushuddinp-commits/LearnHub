CREATE DATABASE IF NOT EXISTS learnhub;
USE learnhub;

-- Spring Boot + Hibernate creates/updates these tables automatically on first run.
-- Conceptual M:N design:
-- students (1) ---- (many) enrollments (many) ---- (1) courses
-- A student can have many enrollment rows; a course can have many enrollment rows.
-- The UNIQUE(student_id, course_id) constraint prevents duplicate enrollment.
