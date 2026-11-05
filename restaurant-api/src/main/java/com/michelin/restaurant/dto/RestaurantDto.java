package com.michelin.restaurant.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class RestaurantDto {

    private Long id;
    private String nom;
    private String adresse;
    private String image;
    private List<EvaluationDto> evaluations;
    private double moyenne;
}