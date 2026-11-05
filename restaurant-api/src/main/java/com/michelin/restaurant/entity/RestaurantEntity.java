package com.michelin.restaurant.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
public class RestaurantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 90)
    private String nom;

    @Column(length = 255)
    private String adresse;

    private String image;

    @OneToMany(mappedBy = "restaurant")
    private List<EvaluationEntity> evaluations = new ArrayList<>();
}