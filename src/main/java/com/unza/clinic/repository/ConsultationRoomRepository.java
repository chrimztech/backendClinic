package com.unza.clinic.repository;

import com.unza.clinic.model.ConsultationRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationRoomRepository extends JpaRepository<ConsultationRoom, Long> {
    Optional<ConsultationRoom> findByRoomCodeIgnoreCase(String roomCode);
    List<ConsultationRoom> findByDepartmentIgnoreCaseOrderByName(String department);
}
