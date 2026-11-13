package com.learnhub.config;

import com.learnhub.model.Course;
import com.learnhub.repository.CourseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedCourses(CourseRepository courses) {
        return args -> {
            List<Course> catalogue = List.of(
                new Course("Python for Beginners", "Programming", "Learn Python fundamentals, variables, functions and problem solving.", "LearnHub Faculty", 6),
                new Course("Java Programming", "Programming", "Understand object-oriented programming, classes, inheritance and collections.", "LearnHub Faculty", 8),
                new Course("Database Management Systems", "Database", "Learn SQL, relational design, keys and database relationships.", "LearnHub Faculty", 5),
                new Course("Machine Learning Foundations", "Artificial Intelligence", "Explore datasets, model training, evaluation and core ML concepts.", "LearnHub Faculty", 8),
                new Course("Web Development", "Development", "Build responsive websites using HTML, CSS and JavaScript.", "LearnHub Faculty", 6),
                new Course("Data Structures and Algorithms", "Computer Science", "Practice arrays, linked lists, stacks, queues, trees and algorithms.", "LearnHub Faculty", 7),
                new Course("Generative AI Essentials", "Artificial Intelligence", "Understand generative AI, language models, prompting and responsible use.", "LearnHub Faculty", 4),
                new Course("Agentic AI for Beginners", "Artificial Intelligence", "Learn how AI agents plan tasks, use tools and complete multi-step workflows.", "LearnHub Faculty", 5),
                new Course("Prompt Engineering", "Artificial Intelligence", "Write clear prompts and evaluate useful, reliable AI responses.", "LearnHub Faculty", 3),
                new Course("Cloud Computing Basics", "Cloud Computing", "Explore cloud services, deployment models, storage and scalability.", "LearnHub Faculty", 5),
                new Course("Cybersecurity Fundamentals", "Cybersecurity", "Learn safe computing, authentication, network basics and common threats.", "LearnHub Faculty", 6),
                new Course("C Programming", "Programming", "Build programming logic with data types, conditions, loops and functions.", "LearnHub Faculty", 5),
                new Course("C++ and Object-Oriented Design", "Programming", "Practice classes, objects, inheritance, polymorphism and STL basics.", "LearnHub Faculty", 6),
                new Course("React for Frontend Development", "Development", "Create component-based interfaces with React, state and routing.", "LearnHub Faculty", 6),
                new Course("Spring Boot API Development", "Development", "Build REST APIs with Spring Boot, validation and database access.", "LearnHub Faculty", 7),
                new Course("UI/UX Design Fundamentals", "Design", "Learn user research, wireframes, visual hierarchy and prototyping.", "LearnHub Faculty", 4),
                new Course("Data Analytics with Python", "Data Science", "Explore data cleaning, visualization and beginner analytics workflows.", "LearnHub Faculty", 6),
                new Course("Statistics for Data Science", "Data Science", "Understand probability, distributions, sampling and basic inference.", "LearnHub Faculty", 5),
                new Course("Computer Networks", "Computer Science", "Study network layers, addressing, routing and common protocols.", "LearnHub Faculty", 6),
                new Course("Operating Systems", "Computer Science", "Understand processes, threads, memory, scheduling and file systems.", "LearnHub Faculty", 6)
            );
            for (Course course : catalogue) {
                if (!courses.existsByTitleIgnoreCase(course.getTitle())) {
                    courses.save(course);
                }
            }
        };
    }
}
