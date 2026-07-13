package com.unza.clinic.repository;

import com.unza.clinic.model.ImagingRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagingRepository extends JpaRepository<ImagingRequest, Long> {
}