package com.unza.clinic.repository;

import com.unza.clinic.model.StaffSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StaffScheduleRepository extends JpaRepository<StaffSchedule, Long> {
    List<StaffSchedule> findByWeekOf(String weekOf);
}
