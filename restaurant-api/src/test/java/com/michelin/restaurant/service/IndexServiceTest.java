package com.michelin.restaurant.service;

import com.michelin.restaurant.entity.EvaluationEntity;
import com.michelin.restaurant.repository.EvaluationRepository;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IndexServiceTest {
    @TempDir Path dossier;
    IndexService service;

    @BeforeEach
    void preparer() throws Exception {
        var repository = mock(EvaluationRepository.class);
        var evaluation = new EvaluationEntity();
        evaluation.setId(1L);
        evaluation.setCommentaire("Excellent repas et service agréable");
        when(repository.findAll()).thenReturn(List.of(evaluation));
        // Un index temporaire : aucun accès à l'index de l'application.
        var directory = FSDirectory.open(dossier);
        try (var fichiers = mockStatic(FSDirectory.class)) {
            fichiers.when(() -> FSDirectory.open(any(java.nio.file.Path.class)))
                    .thenReturn(directory);
            service = new IndexService(repository);
        }
    }

    @AfterEach
    void fermer() throws Exception {
        if (service != null) service.fermer();
    }

    @Test
    void doitReconstruireIndexDepuisRepository() throws Exception {
        assertEquals(List.of(1L), service.rechercherEvaluations("excellent"));
    }

    @Test
    void doitExigerTousLesMots() throws Exception {
        service.indexerEvaluation(2L, "Repas décevant");
        assertEquals(List.of(1L), service.rechercherEvaluations("excellent repas"));
        assertTrue(service.rechercherEvaluations("excellent décevant").isEmpty());
    }

    @Test
    void doitRemplacerAncienCommentaireSansDoublon() throws Exception {
        service.indexerEvaluation(1L, "Cuisine remarquable");
        assertTrue(service.rechercherEvaluations("excellent").isEmpty());
        assertEquals(List.of(1L), service.rechercherEvaluations("cuisine"));
    }

    @Test
    void doitSupprimerEvaluationDeIndex() throws Exception {
        service.supprimerEvaluation(1L);
        assertTrue(service.rechercherEvaluations("repas").isEmpty());
    }

    @Test
    void doitAccepterRechercheVideEtSansResultat() throws Exception {
        assertTrue(service.rechercherEvaluations(null).isEmpty());
        assertTrue(service.rechercherEvaluations("   ").isEmpty());
        assertTrue(service.rechercherEvaluations("introuvable").isEmpty());
    }

    @Test
    void doitAccepterCaracteresSpeciauxSansErreur() {
        assertDoesNotThrow(() -> service.rechercherEvaluations("repas + (service):"));
    }
}
