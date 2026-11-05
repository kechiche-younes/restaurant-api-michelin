package com.michelin.restaurant.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EvaluationDto {

    private Long id;
    private String evaluateur;
    private String commentaire;
    private int note;
    private List<String> photos;
}