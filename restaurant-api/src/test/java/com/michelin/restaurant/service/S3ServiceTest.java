package com.michelin.restaurant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {
    @Mock S3Client client;
    S3Service service;

    @BeforeEach
    void preparer() {
        service = new S3Service(client, "restaurant", "evaluation");
    }

    @ParameterizedTest
    @ValueSource(strings = {"restaurant", "evaluation"})
    void doitEnvoyerDansLeBonBucket(String bucket) throws Exception {
        byte[] contenu = {1, 2, 3};
        var fichier = new MockMultipartFile("fichier", "photo.png", "image/png", contenu);
        String cle = bucket.equals("restaurant")
                ? service.envoyerImageRestaurant(fichier) : service.envoyerPhotoEvaluation(fichier);
        var requete = ArgumentCaptor.forClass(PutObjectRequest.class);
        var corps = ArgumentCaptor.forClass(RequestBody.class);
        verify(client).putObject(requete.capture(), corps.capture());
        assertEquals(bucket, requete.getValue().bucket());
        assertEquals("image/png", requete.getValue().contentType());
        assertEquals(cle, requete.getValue().key());
        assertTrue(cle.startsWith("images/"));
        assertEquals(contenu.length, corps.getValue().optionalContentLength().orElseThrow());
        try (var flux = corps.getValue().contentStreamProvider().newStream()) {
            assertArrayEquals(contenu, flux.readAllBytes());
        }
    }

    @Test
    void doitRefuserFichierVide() {
        var fichier = new MockMultipartFile("fichier", new byte[0]);
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.envoyerImageRestaurant(fichier)).getStatusCode().value());
        verifyNoInteractions(client);
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/plain", "application/pdf"})
    void doitRefuserFichierAutreQuImage(String type) {
        var fichier = new MockMultipartFile("fichier", "document", type, new byte[]{1});
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.envoyerPhotoEvaluation(fichier)).getStatusCode().value());
        verifyNoInteractions(client);
    }

    @Test
    void doitRefuserTypeAbsent() {
        var fichier = new MockMultipartFile("fichier", "photo", null, new byte[]{1});
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.envoyerPhotoEvaluation(fichier)).getStatusCode().value());
        verifyNoInteractions(client);
    }

    @Test
    void doitGererErreurStockage() {
        var fichier = new MockMultipartFile("fichier", "photo.png", "image/png", new byte[]{1});
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("stockage indisponible"));
        assertEquals(502, assertThrows(ResponseStatusException.class,
                () -> service.envoyerImageRestaurant(fichier)).getStatusCode().value());
    }

    @Test
    void doitGererErreurLecture() throws Exception {
        var fichier = mock(MultipartFile.class);
        when(fichier.getContentType()).thenReturn("image/png");
        when(fichier.getInputStream()).thenThrow(new IOException("lecture impossible"));
        assertEquals(502, assertThrows(ResponseStatusException.class,
                () -> service.envoyerPhotoEvaluation(fichier)).getStatusCode().value());
        verifyNoInteractions(client);
    }
}
