package com.michelin.restaurant.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErreurDto {

    private int code;
    private String message;
}