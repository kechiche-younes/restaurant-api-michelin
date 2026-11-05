
package com.michelin.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RestaurantModificationDto {

    @NotBlank(message = "Le nom ne peut pas être vide.")
    @Size(max = 90, message = "Le nom ne peut pas dépasser 90 caractères.")
    private String nom;

    @NotBlank(message = "L'adresse ne peut pas être vide.")
    @Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères.")
    private String adresse;
}