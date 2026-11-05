package com.michelin.restaurant.service;

import jakarta.annotation.PreDestroy;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.michelin.restaurant.repository.EvaluationRepository;

@Service
public class IndexService {
    private final Directory directory;
    private final StandardAnalyzer analyzer;
    private final IndexWriter indexWriter;

    public IndexService(EvaluationRepository evaluationRepository)
            throws IOException {

        directory = FSDirectory.open(Paths.get("index-evaluations"));
        analyzer = new StandardAnalyzer();

        IndexWriterConfig configuration = new IndexWriterConfig(analyzer);
        indexWriter = new IndexWriter(directory, configuration);

        // Reconstruit l'index à partir des données présentes dans H2.
        indexWriter.deleteAll();
        indexWriter.commit();

        for (var evaluation : evaluationRepository.findAll()) {
            indexerEvaluation(
                    evaluation.getId(),
                    evaluation.getCommentaire()
            );
        }
    }
    public synchronized void indexerEvaluation(
            Long evaluationId,
            String commentaire) throws IOException {

        Document document = new Document();

        document.add(new StringField("evaluationId",evaluationId.toString(),Field.Store.YES));

        document.add(new TextField("commentaire",commentaire,Field.Store.NO));
        indexWriter.updateDocument(new Term("evaluationId", evaluationId.toString()),document);
        indexWriter.commit();
    }

    public synchronized void supprimerEvaluation(Long evaluationId)
            throws IOException {

        indexWriter.deleteDocuments(new Term("evaluationId", evaluationId.toString()));
        indexWriter.commit();
    }

    public synchronized List<Long> rechercherEvaluations(String mots)
            throws IOException, ParseException {

        if (mots == null || mots.isBlank()) {
            return List.of();
        }
        QueryParser parser = new QueryParser("commentaire", analyzer);

        parser.setDefaultOperator(QueryParser.Operator.AND);
        Query requete = parser.parse(QueryParser.escape(mots.trim()));

        try (DirectoryReader reader = DirectoryReader.open(indexWriter)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            if (reader.numDocs() == 0) {
                return List.of();
            }
            TopDocs resultats = searcher.search(requete,reader.numDocs());
            List<Long> evaluationIds = new ArrayList<>();

            for (ScoreDoc resultat : resultats.scoreDocs) {
                Document document = searcher.storedFields().document(resultat.doc);
                evaluationIds.add(Long.valueOf(document.get("evaluationId")));
            }
            return evaluationIds;
        }
    }

    @PreDestroy
    public void fermer() throws IOException {
        try {
            indexWriter.close();
        } finally {
            try {
                directory.close();
            } finally {
                analyzer.close();
            }
        }
    }
}