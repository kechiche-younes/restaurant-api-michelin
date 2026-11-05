package com.michelin.restaurant.service;

import com.michelin.restaurant.dto.RestaurantCreationDto;
import com.michelin.restaurant.dto.RestaurantModificationDto;
import com.michelin.restaurant.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.michelin.restaurant.dto.RestaurantDto;

import com.michelin.restaurant.entity.RestaurantEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.michelin.restaurant.dto.EvaluationDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final S3Service s3Service;

    @Transactional
    public List<RestaurantDto> getRestaurants() {
        return restaurantRepository.findAll()
                .stream()
                .map(this::convertirEnDto)
                .toList();
    }
    @Transactional
    public RestaurantDto getRestaurant(Long id) {
        RestaurantEntity restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Restaurant introuvable"
                ));

        return convertirEnDto(restaurant);
    }


    @Transactional
    public RestaurantDto creerRestaurant(RestaurantCreationDto creationDto) {
        RestaurantEntity entity = new RestaurantEntity();

        entity.setNom(creationDto.getNom());
        entity.setAdresse(creationDto.getAdresse());

        RestaurantEntity restaurantEnregistre =
                restaurantRepository.save(entity);

        return convertirEnDto(restaurantEnregistre);
    }


    @Transactional
    public RestaurantDto modifierRestaurant(
            Long id,
            RestaurantModificationDto modificationDto) {

        RestaurantEntity entity = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Restaurant introuvable"
                ));

        entity.setNom(modificationDto.getNom());
        entity.setAdresse(modificationDto.getAdresse());

        return convertirEnDto(entity);
    }


    @Transactional
    public String ajouterImage(Long restaurantId, MultipartFile fichier) {
        RestaurantEntity restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Restaurant introuvable"
                ));

        String cle = s3Service.envoyerImageRestaurant(fichier);

        restaurant.setImage(cle);

        return cle;
    }

    private RestaurantDto convertirEnDto(RestaurantEntity restaurant) {
        RestaurantDto dto = new RestaurantDto();

        dto.setId(restaurant.getId());
        dto.setNom(restaurant.getNom());
        dto.setAdresse(restaurant.getAdresse());
        dto.setImage(restaurant.getImage());

        List<EvaluationDto> evaluations = restaurant.getEvaluations()
                .stream()
                .map(evaluation -> {
                    EvaluationDto evaluationDto = new EvaluationDto();

                    evaluationDto.setId(evaluation.getId());
                    evaluationDto.setEvaluateur(evaluation.getEvaluateur());
                    evaluationDto.setCommentaire(evaluation.getCommentaire());
                    evaluationDto.setNote(evaluation.getNote());
                    evaluationDto.setPhotos(List.copyOf(evaluation.getPhotos()));

                    return evaluationDto;
                })
                .toList();

        dto.setEvaluations(evaluations);

        double moyenne = restaurant.getEvaluations()
                .stream()
                .mapToInt(evaluation -> evaluation.getNote())
                .average()
                .orElse(-1);

        dto.setMoyenne(moyenne);

        return dto;
    }

}
