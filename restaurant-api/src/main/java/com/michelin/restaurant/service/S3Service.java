package com.michelin.restaurant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class S3Service {

    private final S3Client client;
    private final String bucketRestaurant;
    private final String bucketEvaluation;
    public S3Service(
            S3Client client,
            @Value("${s3.bucket-restaurant}") String bucketRestaurant,
            @Value("${s3.bucket-evaluation}") String bucketEvaluation) {

        this.client = client;
        this.bucketRestaurant = bucketRestaurant;
        this.bucketEvaluation = bucketEvaluation;
    }

    private String envoyerImage(MultipartFile fichier, String bucket) {
        if (fichier.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le fichier est vide"
            );
        }

        String type = fichier.getContentType();

        if (type == null || !type.startsWith("image/")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le fichier doit être une image"
            );
        }

        String cle = "images/" + UUID.randomUUID();

        PutObjectRequest requete = PutObjectRequest.builder()
                .bucket(bucket)
                .key(cle)
                .contentType(type)
                .build();

        try (InputStream contenu = fichier.getInputStream()) {
            client.putObject(
                    requete,
                    RequestBody.fromInputStream(contenu, fichier.getSize())
            );

            return cle;
        } catch (IOException | SdkException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "L'envoi de l'image vers le stockage a échoué",
                    e
            );
        }
    }
    public String envoyerImageRestaurant(MultipartFile fichier) {
        return envoyerImage(fichier, bucketRestaurant);
    }

    public String envoyerPhotoEvaluation(MultipartFile fichier) {
        return envoyerImage(fichier, bucketEvaluation);
    }
}