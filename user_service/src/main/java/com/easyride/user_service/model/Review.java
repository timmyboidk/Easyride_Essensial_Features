package com.easyride.user_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User reviewer;

    @ManyToOne
    private User reviewed;

    private double rating;

    @Enumerated(EnumType.STRING)
    private ReviewType type; // PASSENGER_TO_DRIVER, DRIVER_TO_PASSENGER

    @ElementCollection
    private List<String> tags = new ArrayList<>();

    // Getters and setters (now provided by Lombok)
}