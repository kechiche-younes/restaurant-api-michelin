
package com.michelin.restaurant.controller;

import com.michelin.restaurant.dto.EvaluationCreationDto;
import com.michelin.restaurant.dto.EvaluationDto;
import com.michelin.restaurant.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.michelin.restaurant.dto.EvaluationModificationDto;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{restaurantId}/evaluations")
    @ResponseStatus(HttpStatus.CREATED)
    public Long ajouterEvaluation(
            @PathVariable Long restaurantId,
            @Valid @RequestBody EvaluationCreationDto creationDto,
            @AuthenticationPrincipal Jwt jwt) {

        String utilisateurId = jwt.getSubject();
        String evaluateur = jwt.getClaimAsString("name");

        return evaluationService.ajouterEvaluation(
                restaurantId,
                creationDto,
                utilisateurId,
                evaluateur
        );
    }
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/evaluations/mesevaluation")
    public List<EvaluationDto> getMesEvaluations(@AuthenticationPrincipal Jwt jwt) {

        return evaluationService.getMesEvaluations(jwt.getSubject());
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/evaluations/{evaluationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimerEvaluation(
            @PathVariable Long evaluationId,
            @AuthenticationPrincipal Jwt jwt,
            org.springframework.security.core.Authentication authentication) {

        boolean administrateur = authentication.getAuthorities()
                .stream()
                .anyMatch(droit -> droit.getAuthority().equals("ROLE_ADMIN"));

        evaluationService.supprimerEvaluation(
                evaluationId,
                jwt.getSubject(),
                administrateur
        );
    }


    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping("/evaluations/{evaluationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void modifierEvaluation(
            @PathVariable Long evaluationId,
            @Valid @RequestBody EvaluationModificationDto modificationDto,
            @AuthenticationPrincipal Jwt jwt,
            org.springframework.security.core.Authentication authentication) {

        boolean administrateur = authentication.getAuthorities()
                .stream()
                .anyMatch(droit -> droit.getAuthority().equals("ROLE_ADMIN"));

        evaluationService.modifierEvaluation(
                evaluationId,
                modificationDto,
                jwt.getSubject(),
                administrateur
        );
    }



    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/evaluations/recherche")
    public List<EvaluationDto> rechercherEvaluations(
            @RequestParam String mots) {

        return evaluationService.rechercherEvaluations(mots);
    }


    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping(
            value = "/evaluations/{evaluationId}/photos",
            consumes = "multipart/form-data"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public String ajouterPhoto(
            @PathVariable Long evaluationId,
            @RequestParam("fichier") MultipartFile fichier,
            @AuthenticationPrincipal Jwt jwt,
            org.springframework.security.core.Authentication authentication) {

        boolean administrateur = authentication.getAuthorities()
                .stream()
                .anyMatch(droit -> droit.getAuthority().equals("ROLE_ADMIN"));

        return evaluationService.ajouterPhoto(
                evaluationId,
                fichier,
                jwt.getSubject(),
                administrateur
        );
    }


}
