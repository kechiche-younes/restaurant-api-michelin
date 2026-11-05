package com.michelin.restaurant.exception;

import com.michelin.restaurant.dto.ErreurDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GestionnaireExceptions {
    private static final Logger log = LoggerFactory.getLogger(GestionnaireExceptions.class);
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErreurDto> gererErreurService(
            ResponseStatusException exception) {

        String message = exception.getReason();

        if (message == null) {
            message = "La requête a échoué";
        }

        ErreurDto erreur = new ErreurDto(
                exception.getStatusCode().value(),
                message
        );

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(erreur);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurDto> gererErreurValidation(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(erreur -> erreur.getField()
                        + " : "
                        + erreur.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErreurDto erreur = new ErreurDto(
                HttpStatus.BAD_REQUEST.value(),
                message
        );

        return ResponseEntity
                .badRequest()
                .body(erreur);
    }
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErreurDto> gererFichierManquant(
            MissingServletRequestPartException exception) {

        return ResponseEntity.badRequest().body(
                new ErreurDto(
                        400,
                        "Le champ '" + exception.getRequestPartName()
                                + "' est obligatoire"
                )
        );
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErreurDto> gererJsonInvalide(
            HttpMessageNotReadableException exception) {

        return ResponseEntity.badRequest().body(
                new ErreurDto(400, "Le corps JSON est absent ou invalide")
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErreurDto> gererAutreErreur(
            Exception exception) throws Exception {

        // Laisse Spring Security gérer l'authentification et les droits.
        if (exception instanceof AccessDeniedException
                || exception instanceof AuthenticationException) {
            throw exception;
        }

        // Conserve le statut des erreurs HTTP reconnues par Spring.
        if (exception instanceof ErrorResponse erreurSpring) {
            int code = erreurSpring.getStatusCode().value();

            String message = erreurSpring.getBody().getDetail();

            if (message == null) {
                message = "La requête a échoué";
            }

            return ResponseEntity
                    .status(erreurSpring.getStatusCode())
                    .body(new ErreurDto(code, message));
        }

        log.error("Erreur inattendue pendant le traitement de la requête", exception);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErreurDto(500, "Une erreur interne est survenue"));
    }
}