package com.michelin.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvaluationModificationDto {

    @NotBlank(message = "Le commentaire ne peut pas être vide.")
    @Size(max = 255,message = "Le commentaire ne peut pas dépasser 255 caractères.")
    private String commentaire;

    @NotNull(message = "La note est obligatoire.")
    @Min(value = 0, message = "La note minimale est 0.")
    @Max(value = 3, message = "La note maximale est 3.")
    private Integer note;
}