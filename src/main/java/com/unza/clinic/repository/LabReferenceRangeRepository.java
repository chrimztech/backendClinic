package com.unza.clinic.repository;

import com.unza.clinic.model.LabReferenceRange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface LabReferenceRangeRepository extends JpaRepository<LabReferenceRange, Long> {
    Optional<LabReferenceRange> findByRangeId(String rangeId);
    List<LabReferenceRange> findByTestNameIgnoreCase(String testName);
    List<LabReferenceRange> findByCategoryIgnoreCase(String category);

    @Query("SELECT r FROM LabReferenceRange r WHERE LOWER(r.testName) = LOWER(:testName) AND (r.gender = 'ALL' OR r.gender = :gender) AND r.minAge <= :age AND r.maxAge >= :age")
    List<LabReferenceRange> findApplicableRange(@Param("testName") String testName, @Param("gender") String gender, @Param("age") int age);
}
