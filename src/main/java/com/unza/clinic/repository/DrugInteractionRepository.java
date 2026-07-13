package com.unza.clinic.repository;

import com.unza.clinic.model.DrugInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface DrugInteractionRepository extends JpaRepository<DrugInteraction, Long> {
    Optional<DrugInteraction> findByInteractionId(String interactionId);

    @Query("SELECT d FROM DrugInteraction d WHERE LOWER(d.drugA) = LOWER(:drug) OR LOWER(d.drugB) = LOWER(:drug)")
    List<DrugInteraction> findByDrug(@Param("drug") String drug);

    @Query("SELECT d FROM DrugInteraction d WHERE (LOWER(d.drugA) = LOWER(:drugA) AND LOWER(d.drugB) = LOWER(:drugB)) OR (LOWER(d.drugA) = LOWER(:drugB) AND LOWER(d.drugB) = LOWER(:drugA))")
    List<DrugInteraction> findByDrugPair(@Param("drugA") String drugA, @Param("drugB") String drugB);

    List<DrugInteraction> findBySeverity(String severity);
}
