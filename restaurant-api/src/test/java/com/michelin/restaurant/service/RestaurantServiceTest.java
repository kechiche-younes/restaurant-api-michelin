
package com.michelin.restaurant.service;

import com.michelin.restaurant.dto.RestaurantCreationDto;
import com.michelin.restaurant.dto.RestaurantDto;
import com.michelin.restaurant.dto.RestaurantModificationDto;
import com.michelin.restaurant.entity.EvaluationEntity;
import com.michelin.restaurant.entity.RestaurantEntity;
import com.michelin.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private RestaurantService restaurantService;

    @Test
    void doitRetournerLesRestaurants() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setNom("Le Gourmet");

        when(restaurantRepository.findAll())
                .thenReturn(List.of(restaurant));

        List<RestaurantDto> resultat =
                restaurantService.getRestaurants();

        assertEquals(1, resultat.size());
        assertEquals("Le Gourmet", resultat.get(0).getNom());
        assertEquals(-1.0, resultat.get(0).getMoyenne());
        assertEquals(List.of(), resultat.get(0).getEvaluations());
    }

    @Test
    void doitRetournerUneListeVide() {
        when(restaurantRepository.findAll())
                .thenReturn(List.of());

        List<RestaurantDto> resultat =
                restaurantService.getRestaurants();

        assertEquals(0, resultat.size());
    }

    @Test
    void doitCalculerLaMoyenneDesEvaluations() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setNom("Le Gourmet");

        EvaluationEntity evaluation1 = new EvaluationEntity();
        evaluation1.setId(1L);
        evaluation1.setCommentaire("Bon restaurant");
        evaluation1.setNote(1);
        evaluation1.getPhotos().add("images/photo-1");

        EvaluationEntity evaluation2 = new EvaluationEntity();
        evaluation2.setId(2L);
        evaluation2.setNote(2);

        restaurant.getEvaluations().add(evaluation1);
        restaurant.getEvaluations().add(evaluation2);

        when(restaurantRepository.findAll())
                .thenReturn(List.of(restaurant));

        List<RestaurantDto> resultat =
                restaurantService.getRestaurants();

        assertEquals(1.5, resultat.get(0).getMoyenne());
        assertEquals(2, resultat.get(0).getEvaluations().size());

        assertEquals(
                "Bon restaurant",
                resultat.get(0).getEvaluations().get(0).getCommentaire()
        );

        assertEquals(
                List.of("images/photo-1"),
                resultat.get(0).getEvaluations().get(0).getPhotos()
        );
    }

    @Test
    void doitRetournerUnRestaurantParId() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(1L);
        restaurant.setNom("Le Gourmet");
        restaurant.setAdresse("10 rue de Paris");

        when(restaurantRepository.findById(1L))
                .thenReturn(Optional.of(restaurant));

        RestaurantDto resultat =
                restaurantService.getRestaurant(1L);

        assertEquals(1L, resultat.getId());
        assertEquals("Le Gourmet", resultat.getNom());
        assertEquals("10 rue de Paris", resultat.getAdresse());
        assertEquals(-1.0, resultat.getMoyenne());
        assertEquals(List.of(), resultat.getEvaluations());
    }

    @Test
    void doitRetourner404SiRestaurantAbsent() {
        when(restaurantRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> restaurantService.getRestaurant(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, erreur.getStatusCode());
        assertEquals("Restaurant introuvable", erreur.getReason());
    }

    @Test
    void doitCreerUnRestaurantSansEvaluation() {
        RestaurantCreationDto creationDto = new RestaurantCreationDto();
        creationDto.setNom("L'orientale");
        creationDto.setAdresse("4 impasse de l'épi");

        when(restaurantRepository.save(any(RestaurantEntity.class)))
                .thenAnswer(invocation -> {
                    RestaurantEntity restaurant = invocation.getArgument(0);
                    restaurant.setId(1L);
                    return restaurant;
                });

        RestaurantDto resultat =
                restaurantService.creerRestaurant(creationDto);

        assertEquals(1L, resultat.getId());
        assertEquals("L'orientale", resultat.getNom());
        assertEquals("4 impasse de l'épi", resultat.getAdresse());
        assertEquals(-1.0, resultat.getMoyenne());
        assertEquals(List.of(), resultat.getEvaluations());

        verify(restaurantRepository)
                .save(any(RestaurantEntity.class));
    }

    @Test
    void doitModifierUnRestaurant() {
        RestaurantEntity restaurant = new RestaurantEntity();
        restaurant.setId(1L);
        restaurant.setNom("Ancien nom");
        restaurant.setAdresse("Ancienne adresse");

        RestaurantModificationDto modificationDto =
                new RestaurantModificationDto();

        modificationDto.setNom("L'orientale");
        modificationDto.setAdresse("4 impasse de l'épi");

        when(restaurantRepository.findById(1L))
                .thenReturn(Optional.of(restaurant));

        RestaurantDto resultat = restaurantService.modifierRestaurant(
                1L,
                modificationDto
        );

        assertEquals(1L, resultat.getId());
        assertEquals("L'orientale", resultat.getNom());
        assertEquals("4 impasse de l'épi", resultat.getAdresse());
        assertEquals(-1.0, resultat.getMoyenne());

        assertEquals("L'orientale", restaurant.getNom());
        assertEquals("4 impasse de l'épi", restaurant.getAdresse());
    }

    @Test
    void doitRetourner404SiRestaurantAModifierAbsent() {
        RestaurantModificationDto modificationDto =
                new RestaurantModificationDto();

        modificationDto.setNom("L'orientale");
        modificationDto.setAdresse("4 impasse de l'épi");

        when(restaurantRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException erreur = assertThrows(
                ResponseStatusException.class,
                () -> restaurantService.modifierRestaurant(
                        99L,
                        modificationDto
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, erreur.getStatusCode());
        assertEquals("Restaurant introuvable", erreur.getReason());
    }
    @Test
    void doitAjouterImageAuRestaurant() {
        var restaurant = new RestaurantEntity();
        var fichier = new org.springframework.mock.web.MockMultipartFile(
                "fichier", "restaurant.png", "image/png", new byte[]{1});
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(s3Service.envoyerImageRestaurant(fichier)).thenReturn("images/restaurant");
        assertEquals("images/restaurant", restaurantService.ajouterImage(1L, fichier));
        assertEquals("images/restaurant", restaurant.getImage());
    }

    @Test
    void doitRefuserImageSiRestaurantAbsent() {
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());
        var erreur = assertThrows(ResponseStatusException.class,
                () -> restaurantService.ajouterImage(99L, null));
        assertEquals(HttpStatus.NOT_FOUND, erreur.getStatusCode());
        org.mockito.Mockito.verifyNoInteractions(s3Service);
    }

}