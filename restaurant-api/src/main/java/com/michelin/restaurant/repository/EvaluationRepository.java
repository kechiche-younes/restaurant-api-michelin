package com.michelin.restaurant.repository;

import com.michelin.restaurant.entity.EvaluationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationRepository extends JpaRepository<EvaluationEntity, Long> {
    List<EvaluationEntity> findByUtilisateurId(String utilisateurId);
}
