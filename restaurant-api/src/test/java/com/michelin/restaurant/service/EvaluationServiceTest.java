package com.michelin.restaurant.service;

import com.michelin.restaurant.dto.*;
import com.michelin.restaurant.entity.*;
import com.michelin.restaurant.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {
    @Mock EvaluationRepository evaluationRepository;
    @Mock RestaurantRepository restaurantRepository;
    @Mock IndexService indexService;
    @Mock S3Service s3Service;
    @InjectMocks EvaluationService service;

    private EvaluationCreationDto creation() {
        var dto = new EvaluationCreationDto();
        dto.setCommentaire("Excellent repas");
        dto.setNote(3);
        return dto;
    }

    private EvaluationModificationDto modification() {
        var dto = new EvaluationModificationDto();
        dto.setCommentaire("Service agréable");
        dto.setNote(2);
        return dto;
    }

    private EvaluationEntity evaluation() {
        var entity = new EvaluationEntity();
        entity.setId(10L);
        entity.setUtilisateurId("proprietaire");
        entity.setEvaluateur("Lucien");
        entity.setCommentaire("Excellent repas");
        entity.setNote(3);
        entity.getPhotos().add("images/plat");
        return entity;
    }

    @Test
    void doitCreerEtIndexerEvaluation() throws Exception {
        var restaurant = new RestaurantEntity();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(evaluationRepository.save(any())).thenAnswer(invocation -> {
            EvaluationEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });
        assertEquals(10L, service.ajouterEvaluation(1L, creation(), "proprietaire", "Lucien"));
        var capture = ArgumentCaptor.forClass(EvaluationEntity.class);
        verify(evaluationRepository).save(capture.capture());
        var entity = capture.getValue();
        assertEquals("Lucien", entity.getEvaluateur());
        assertEquals("proprietaire", entity.getUtilisateurId());
        assertEquals(3, entity.getNote());
        assertSame(restaurant, entity.getRestaurant());
        assertEquals(List.of(entity), restaurant.getEvaluations());
        verify(indexService).indexerEvaluation(10L, "Excellent repas");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void doitRefuserNomAbsent(String nom) {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.ajouterEvaluation(1L, creation(), "user", nom)).getStatusCode().value());
        verifyNoInteractions(restaurantRepository, evaluationRepository, indexService);
    }

    @Test
    void doitRefuserNomTropLong() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.ajouterEvaluation(1L, creation(), "user", "a".repeat(51))).getStatusCode().value());
        verifyNoInteractions(evaluationRepository);
    }

    @Test
    void doitRefuserCreationSurRestaurantAbsent() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.ajouterEvaluation(1L, creation(), "user", "Lucien")).getStatusCode().value());
        verifyNoInteractions(evaluationRepository, indexService);
    }

    @Test
    void doitRetournerMesEvaluationsAvecPhotos() {
        when(evaluationRepository.findByUtilisateurId("proprietaire")).thenReturn(List.of(evaluation()));
        var resultats = service.getMesEvaluations("proprietaire");
        assertEquals(1, resultats.size());
        var dto = resultats.get(0);
        assertEquals(10L, dto.getId());
        assertEquals("Lucien", dto.getEvaluateur());
        assertEquals("Excellent repas", dto.getCommentaire());
        assertEquals(3, dto.getNote());
        assertEquals(List.of("images/plat"), dto.getPhotos());
    }

    @Test
    void doitRetournerMesEvaluationsVides() {
        when(evaluationRepository.findByUtilisateurId("user")).thenReturn(List.of());
        assertTrue(service.getMesEvaluations("user").isEmpty());
    }

    // Le propriétaire et l'administrateur peuvent agir ; un autre utilisateur ne le peut pas.
    @ParameterizedTest
    @CsvSource({"proprietaire,false", "autre,true"})
    void doitAutoriserModification(String utilisateur, boolean admin) throws Exception {
        var entity = evaluation();
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(entity));
        service.modifierEvaluation(10L, modification(), utilisateur, admin);
        assertEquals(2, entity.getNote());
        assertEquals("Service agréable", entity.getCommentaire());
        verify(indexService).indexerEvaluation(10L, "Service agréable");
    }

    @ParameterizedTest
    @CsvSource({"proprietaire,false", "autre,true"})
    void doitAutoriserSuppression(String utilisateur, boolean admin) throws Exception {
        var entity = evaluation();
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(entity));
        service.supprimerEvaluation(10L, utilisateur, admin);
        verify(evaluationRepository).delete(entity);
        verify(indexService).supprimerEvaluation(10L);
    }

    @ParameterizedTest
    @CsvSource({"proprietaire,false", "autre,true"})
    void doitAutoriserAjoutPhoto(String utilisateur, boolean admin) {
        var entity = evaluation();
        var fichier = new MockMultipartFile("fichier", "plat.png", "image/png", new byte[]{1});
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(entity));
        when(s3Service.envoyerPhotoEvaluation(fichier)).thenReturn("images/nouveau");
        assertEquals("images/nouveau", service.ajouterPhoto(10L, fichier, utilisateur, admin));
        assertEquals(List.of("images/plat", "images/nouveau"), entity.getPhotos());
    }

    @Test
    void doitRefuserActionsSurEvaluationAutrui() {
        var entity = evaluation();
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(entity));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.modifierEvaluation(10L, modification(), "autre", false)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.supprimerEvaluation(10L, "autre", false)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.ajouterPhoto(10L, null, "autre", false)).getStatusCode().value());
        assertEquals(3, entity.getNote());
        verify(evaluationRepository, never()).delete(any());
        verifyNoInteractions(indexService, s3Service);
    }

    @Test
    void doitRetourner404PourEvaluationAbsente() {
        when(evaluationRepository.findById(10L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.modifierEvaluation(10L, modification(), "user", true)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.supprimerEvaluation(10L, "user", true)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.ajouterPhoto(10L, null, "user", true)).getStatusCode().value());
        verifyNoInteractions(indexService, s3Service);
    }

    @Test
    void doitRechercherEvaluations() throws Exception {
        when(indexService.rechercherEvaluations("excellent repas")).thenReturn(List.of(10L));
        when(evaluationRepository.findAllById(List.of(10L))).thenReturn(List.of(evaluation()));
        var resultats = service.rechercherEvaluations("excellent repas");
        assertEquals(1, resultats.size());
        assertEquals("Excellent repas", resultats.get(0).getCommentaire());
        assertEquals(List.of("images/plat"), resultats.get(0).getPhotos());
    }

    @Test
    void doitGererErreurRecherche() throws Exception {
        when(indexService.rechercherEvaluations("repas")).thenThrow(new IOException("index indisponible"));
        assertEquals(500, assertThrows(ResponseStatusException.class,
                () -> service.rechercherEvaluations("repas")).getStatusCode().value());
    }

    @Test
    void doitGererErreurSuppressionIndex() throws Exception {
        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(evaluation()));
        doThrow(new IOException("index indisponible")).when(indexService).supprimerEvaluation(10L);
        assertEquals(500, assertThrows(ResponseStatusException.class,
                () -> service.supprimerEvaluation(10L, "proprietaire", false)).getStatusCode().value());
    }
}
