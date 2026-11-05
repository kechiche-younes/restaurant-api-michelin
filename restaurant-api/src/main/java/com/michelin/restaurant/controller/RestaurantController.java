package com.michelin.restaurant.controller;


import com.michelin.restaurant.dto.RestaurantCreationDto;
import com.michelin.restaurant.dto.RestaurantDto;
import com.michelin.restaurant.dto.RestaurantModificationDto;
import com.michelin.restaurant.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping("/restaurants")
    public List<RestaurantDto> getRestaurant(){
        return  restaurantService.getRestaurants();
    }

    @GetMapping("/restaurants/{id}")
    public RestaurantDto getRestaurantById(@PathVariable Long id) {
        return restaurantService.getRestaurant(id);
    }



    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/restaurants")
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantDto creerRestaurant(
            @Valid @RequestBody RestaurantCreationDto creationDto) {

        return restaurantService.creerRestaurant(creationDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/restaurants/{id}")
    public RestaurantDto modifierRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantModificationDto modificationDto) {

        return restaurantService.modifierRestaurant(id, modificationDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(
            value = "/restaurants/{restaurantId}/image",
            consumes = "multipart/form-data"
    )
    public String ajouterImage(
            @PathVariable Long restaurantId,
            @RequestParam("fichier") MultipartFile fichier) {

        return restaurantService.ajouterImage(restaurantId, fichier);
    }

}
