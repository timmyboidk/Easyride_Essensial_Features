package com.easyride.userservice.models;

@Entity
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

    // Getters and setters
}