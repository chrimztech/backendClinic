package com.unza.clinic.repository;

import com.unza.clinic.model.StaffMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<StaffMember, Long> {
    Optional<StaffMember> findByStaffId(String staffId);
    Optional<StaffMember> findByManNumber(String manNumber);
}
