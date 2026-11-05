
package com.michelin.restaurant.service;

import com.michelin.restaurant.dto.EvaluationCreationDto;
import com.michelin.restaurant.entity.EvaluationEntity;
import com.michelin.restaurant.entity.RestaurantEntity;
import com.michelin.restaurant.repository.EvaluationRepository;
import com.michelin.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.michelin.restaurant.dto.EvaluationDto;
import java.util.List;
import com.michelin.restaurant.dto.EvaluationModificationDto;
import java.io.IOException;
import org.apache.lucene.queryparser.classic.ParseException;

import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final RestaurantRepository restaurantRepository;
    private final IndexService indexService;
    private final S3Service s3Service;

    @Transactional
    public Long ajouterEvaluation(Long restaurantId,EvaluationCreationDto creationDto,String utilisateurId,String evaluateur) {

        if (evaluateur == null || evaluateur.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le nom de l'évaluateur est obligatoire"
            );
        }

        if (evaluateur.length() > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le nom de l'évaluateur ne peut pas dépasser 50 caractères"
            );
        }
        RestaurantEntity restaurant =
                restaurantRepository.findById(restaurantId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Restaurant introuvable"
                        ));

        EvaluationEntity evaluation = new EvaluationEntity();

        evaluation.setCommentaire(creationDto.getCommentaire());
        evaluation.setNote(creationDto.getNote());
        evaluation.setUtilisateurId(utilisateurId);
        evaluation.setEvaluateur(evaluateur);
        evaluation.setRestaurant(restaurant);

        EvaluationEntity evaluationEnregistree =
                evaluationRepository.save(evaluation);

        restaurant.getEvaluations().add(evaluationEnregistree);

        try {
            indexService.indexerEvaluation(
                    evaluationEnregistree.getId(),
                    evaluationEnregistree.getCommentaire()
            );
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible d'indexer l'évaluation",
                    e
            );
        }
        return evaluationEnregistree.getId();
    }

    @Transactional(readOnly = true)
    public List<EvaluationDto> getMesEvaluations(String utilisateurId) {
        return evaluationRepository.findByUtilisateurId(utilisateurId)
                .stream()
                .map(entity -> {
                    EvaluationDto dto = new EvaluationDto();

                    dto.setId(entity.getId());
                    dto.setEvaluateur(entity.getEvaluateur());
                    dto.setCommentaire(entity.getCommentaire());
                    dto.setNote(entity.getNote());
                    dto.setPhotos(entity.getPhotos());

                    return dto;
                })
                .toList();
    }

    @Transactional
    public void supprimerEvaluation(
            Long evaluationId,
            String utilisateurId,
            boolean administrateur) {

        EvaluationEntity evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Évaluation introuvable"
                ));

        if (!administrateur
                && !utilisateurId.equals(evaluation.getUtilisateurId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Vous ne pouvez supprimer que vos propres évaluations"
            );
        }

        evaluationRepository.delete(evaluation);


        try {
            indexService.supprimerEvaluation(evaluationId);
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de retirer l'évaluation de l'index",
                    e
            );
        }
    }


    @Transactional
    public void modifierEvaluation(
            Long evaluationId,
            EvaluationModificationDto modificationDto,
            String utilisateurId,
            boolean administrateur) {

        EvaluationEntity evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Évaluation introuvable"
                ));

        if (!administrateur
                && !utilisateurId.equals(evaluation.getUtilisateurId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Vous ne pouvez modifier que vos propres évaluations"
            );
        }

        evaluation.setCommentaire(modificationDto.getCommentaire());
        evaluation.setNote(modificationDto.getNote());


        try {
            indexService.indexerEvaluation(
                    evaluation.getId(),
                    evaluation.getCommentaire()
            );
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de mettre à jour l'index de l'évaluation",
                    e
            );
        }

    }

    @Transactional(readOnly = true)
    public List<EvaluationDto> rechercherEvaluations(String mots) {
        try {
            List<Long> evaluationIds =
                    indexService.rechercherEvaluations(mots);

            return evaluationRepository.findAllById(evaluationIds)
                    .stream()
                    .map(entity -> {
                        EvaluationDto dto = new EvaluationDto();

                        dto.setId(entity.getId());
                        dto.setEvaluateur(entity.getEvaluateur());
                        dto.setCommentaire(entity.getCommentaire());
                        dto.setNote(entity.getNote());
                        dto.setPhotos(entity.getPhotos());

                        return dto;
                    })
                    .toList();

        } catch (ParseException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Les mots recherchés sont invalides",
                    e
            );
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "La recherche des évaluations a échoué",
                    e
            );
        }
    }
    @Transactional
    public String ajouterPhoto(
            Long evaluationId,
            MultipartFile fichier,
            String utilisateurId,
            boolean administrateur) {

        EvaluationEntity evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Évaluation introuvable"
                ));

        if (!administrateur && !utilisateurId.equals(evaluation.getUtilisateurId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Vous ne pouvez ajouter des photos qu'à vos propres évaluations"
            );
        }
        String cle = s3Service.envoyerPhotoEvaluation(fichier);
        evaluation.getPhotos().add(cle);
        return cle;
    }

}