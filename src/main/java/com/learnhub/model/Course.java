package com.learnhub.model;

import jakarta.persistence.*;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 1200)
    private String description;

    @Column(nullable = false, length = 80)
    private String instructor;

    @Column(nullable = false)
    private Integer durationWeeks;

    protected Course() {}

    public Course(String title, String category, String description, String instructor, Integer durationWeeks) {
        this.title = title;
        this.category = category;
        this.description = description;
        this.instructor = instructor;
        this.durationWeeks = durationWeeks;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getInstructor() { return instructor; }
    public Integer getDurationWeeks() { return durationWeeks; }
    public void setTitle(String title) { this.title = title; }
    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setInstructor(String instructor) { this.instructor = instructor; }
    public void setDurationWeeks(Integer durationWeeks) { this.durationWeeks = durationWeeks; }
}
