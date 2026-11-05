package com.michelin.restaurant.repository;

import com.michelin.restaurant.entity.RestaurantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository
        extends JpaRepository<RestaurantEntity, Long> {
}