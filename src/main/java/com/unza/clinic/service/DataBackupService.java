package com.unza.clinic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unza.clinic.model.*;
import com.unza.clinic.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DataBackupService {

    private final PatientRepository patientRepo;
    private final StaffRepository staffRepo;
    private final DepartmentRepository departmentRepo;
    private final AppointmentRepository appointmentRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final AdmissionRepository admissionRepo;
    private final UserRepository userRepo;
    private final LabTestRepository labTestRepo;
    private final BillingRepository billingRepo;
    private final InventoryRepository inventoryRepo;
    private final SupplierRepository supplierRepo;
    private final DrugRepository drugRepo;
    private final ImagingRepository imagingRepo;
    private final InsuranceClaimRepository claimRepo;
    private final ReferralRepository referralRepo;
    private final TriageRepository triageRepo;
    private final QueueRepository queueRepo;
    private final EmergencyRepository emergencyRepo;
    private final BloodUnitRepository bloodUnitRepo;
    private final DonationRepository donationRepo;
    private final NotificationRepository notificationRepo;
    private final AuditLogRepository auditLogRepo;
    private final AttendanceRepository attendanceRepo;
    private final StaffScheduleRepository staffScheduleRepo;
    private final ClinicalFormRecordRepository clinicalFormRepo;
    private final WardStatusRepository wardRepo;
    private final SystemSettingsRepository settingsRepo;
    private final EncounterRepository encounterRepo;
    private final ServiceTariffRepository tariffRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${backup.directory:./backups}")
    private String backupDirectory;

    @Autowired
    public DataBackupService(
            PatientRepository patientRepo, StaffRepository staffRepo,
            DepartmentRepository departmentRepo, AppointmentRepository appointmentRepo,
            PrescriptionRepository prescriptionRepo, AdmissionRepository admissionRepo,
            UserRepository userRepo, LabTestRepository labTestRepo,
            BillingRepository billingRepo, InventoryRepository inventoryRepo,
            SupplierRepository supplierRepo, DrugRepository drugRepo,
            ImagingRepository imagingRepo, InsuranceClaimRepository claimRepo,
            ReferralRepository referralRepo, TriageRepository triageRepo,
            QueueRepository queueRepo, EmergencyRepository emergencyRepo,
            BloodUnitRepository bloodUnitRepo, DonationRepository donationRepo,
            NotificationRepository notificationRepo, AuditLogRepository auditLogRepo,
            AttendanceRepository attendanceRepo, StaffScheduleRepository staffScheduleRepo,
            ClinicalFormRecordRepository clinicalFormRepo, WardStatusRepository wardRepo,
            SystemSettingsRepository settingsRepo, EncounterRepository encounterRepo,
            ServiceTariffRepository tariffRepo) {
        this.patientRepo = patientRepo;
        this.staffRepo = staffRepo;
        this.departmentRepo = departmentRepo;
        this.appointmentRepo = appointmentRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.admissionRepo = admissionRepo;
        this.userRepo = userRepo;
        this.labTestRepo = labTestRepo;
        this.billingRepo = billingRepo;
        this.inventoryRepo = inventoryRepo;
        this.supplierRepo = supplierRepo;
        this.drugRepo = drugRepo;
        this.imagingRepo = imagingRepo;
        this.claimRepo = claimRepo;
        this.referralRepo = referralRepo;
        this.triageRepo = triageRepo;
        this.queueRepo = queueRepo;
        this.emergencyRepo = emergencyRepo;
        this.bloodUnitRepo = bloodUnitRepo;
        this.donationRepo = donationRepo;
        this.notificationRepo = notificationRepo;
        this.auditLogRepo = auditLogRepo;
        this.attendanceRepo = attendanceRepo;
        this.staffScheduleRepo = staffScheduleRepo;
        this.clinicalFormRepo = clinicalFormRepo;
        this.wardRepo = wardRepo;
        this.settingsRepo = settingsRepo;
        this.encounterRepo = encounterRepo;
        this.tariffRepo = tariffRepo;
    }

    /**
     * Creates a comprehensive backup of ALL clinic data.
     * Returns a map containing all data organized by entity type.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> createFullBackup() {
        Map<String, Object> backup = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        backup.put("backup_metadata", createBackupMetadata(now, formatter));
        backup.put("system_settings", toMapList(settingsRepo.findAll()));
        backup.put("patients", toMapList(patientRepo.findAll()));
        backup.put("staff_members", toMapList(staffRepo.findAll()));
        backup.put("departments", toMapList(departmentRepo.findAll()));
        backup.put("users", toMapList(userRepo.findAll()));
        backup.put("encounters", toMapList(encounterRepo.findAll()));
        backup.put("triages", toMapList(triageRepo.findAll()));
        backup.put("emergency_cases", toMapList(emergencyRepo.findAll()));
        backup.put("appointments", toMapList(appointmentRepo.findAll()));
        backup.put("prescriptions", toMapList(prescriptionRepo.findAll()));
        backup.put("admissions", toMapList(admissionRepo.findAll()));
        backup.put("lab_tests", toMapList(labTestRepo.findAll()));
        backup.put("imaging_requests", toMapList(imagingRepo.findAll()));
        backup.put("clinical_forms", toMapList(clinicalFormRepo.findAll()));
        backup.put("billing_invoices", toMapList(billingRepo.findAll()));
        backup.put("inventory_records", toMapList(inventoryRepo.findAll()));
        backup.put("drugs", toMapList(drugRepo.findAll()));
        backup.put("suppliers", toMapList(supplierRepo.findAll()));
        backup.put("referrals", toMapList(referralRepo.findAll()));
        backup.put("insurance_claims", toMapList(claimRepo.findAll()));
        backup.put("blood_units", toMapList(bloodUnitRepo.findAll()));
        backup.put("donations", toMapList(donationRepo.findAll()));
        backup.put("queue_tickets", toMapList(queueRepo.findAll()));
        backup.put("notifications", toMapList(notificationRepo.findAll()));
        backup.put("attendance_records", toMapList(attendanceRepo.findAll()));
        backup.put("staff_schedules", toMapList(staffScheduleRepo.findAll()));
        backup.put("wards", toMapList(wardRepo.findAll()));
        backup.put("service_tariffs", toMapList(tariffRepo.findAll()));
        backup.put("audit_logs", toMapList(auditLogRepo.findAll()));

        return backup;
    }

    private Map<String, Object> createBackupMetadata(LocalDateTime now, DateTimeFormatter formatter) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("backup_id", "BKP-" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        metadata.put("created_at", now.format(formatter));
        metadata.put("version", "2.0");
        metadata.put("format", "unza-clinic-backup");

        Map<String, Long> recordCounts = new LinkedHashMap<>();
        recordCounts.put("patients", patientRepo.count());
        recordCounts.put("staff", staffRepo.count());
        recordCounts.put("users", userRepo.count());
        recordCounts.put("encounters", encounterRepo.count());
        recordCounts.put("triages", triageRepo.count());
        recordCounts.put("clinical_forms", clinicalFormRepo.count());
        recordCounts.put("billing_invoices", billingRepo.count());
        recordCounts.put("prescriptions", prescriptionRepo.count());
        recordCounts.put("total", 
            patientRepo.count() + staffRepo.count() + userRepo.count() + 
            encounterRepo.count() + triageRepo.count() + clinicalFormRepo.count() +
            billingRepo.count() + prescriptionRepo.count() + admissionRepo.count() +
            labTestRepo.count() + imagingRepo.count() + emergencyRepo.count());
        metadata.put("record_counts", recordCounts);

        return metadata;
    }

    private List<Map<String, Object>> toMapList(List<?> entities) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object entity : entities) {
            result.add(objectMapper.convertValue(entity, Map.class));
        }
        return result;
    }

    /**
     * Automated scheduled backup - runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void performScheduledBackup() {
        SystemSettings settings = settingsRepo.findById(1L).orElse(null);
        if (settings != null && Boolean.TRUE.equals(settings.getBackupEnabled())) {
            try {
                Map<String, Object> backup = createFullBackup();
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(backup);
                Path dir = Paths.get(backupDirectory);
                Files.createDirectories(dir);
                Path file = dir.resolve("unza-clinic-backup-" + timestamp + ".json");
                Files.writeString(file, json, StandardCharsets.UTF_8);
                settings.setLastBackupAt(LocalDateTime.now().toString());
                settingsRepo.save(settings);
                System.out.println("Scheduled backup written to: " + file.toAbsolutePath());
            } catch (Exception e) {
                System.err.println("Scheduled backup failed: " + e.getMessage());
            }
        }
    }

    /**
     * Restore data from a backup
     * WARNING: This will replace existing data!
     */
    @Transactional
    public Map<String, Object> restoreFromBackup(Map<String, Object> backupData) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        // Clear all existing data (careful order to avoid FK violations)
        auditLogRepo.deleteAll();
        notificationRepo.deleteAll();
        queueRepo.deleteAll();
        emergencyRepo.deleteAll();
        triageRepo.deleteAll();
        clinicalFormRepo.deleteAll();
        admissionRepo.deleteAll();
        prescriptionRepo.deleteAll();
        appointmentRepo.deleteAll();
        billingRepo.deleteAll();
        labTestRepo.deleteAll();
        imagingRepo.deleteAll();
        claimRepo.deleteAll();
        referralRepo.deleteAll();
        bloodUnitRepo.deleteAll();
        donationRepo.deleteAll();
        inventoryRepo.deleteAll();
        drugRepo.deleteAll();
        supplierRepo.deleteAll();
        encounterRepo.deleteAll();
        wardRepo.deleteAll();
        staffScheduleRepo.deleteAll();
        attendanceRepo.deleteAll();
        tariffRepo.deleteAll();
        settingsRepo.deleteAll();
        userRepo.deleteAll();
        staffRepo.deleteAll();
        departmentRepo.deleteAll();
        patientRepo.deleteAll();

        // Restore in dependency order
        result.put("settings", restoreSettings((List<Map<String, Object>>) backupData.get("system_settings")));
        result.put("departments", restoreList(departmentRepo, (List<Map<String, Object>>) backupData.get("departments"), Department.class));
        result.put("staff", restoreList(staffRepo, (List<Map<String, Object>>) backupData.get("staff_members"), StaffMember.class));
        result.put("patients", restoreList(patientRepo, (List<Map<String, Object>>) backupData.get("patients"), Patient.class));
        result.put("users", restoreList(userRepo, (List<Map<String, Object>>) backupData.get("users"), AppUser.class));
        result.put("tariffs", restoreList(tariffRepo, (List<Map<String, Object>>) backupData.get("service_tariffs"), ServiceTariff.class));
        result.put("encounters", restoreList(encounterRepo, (List<Map<String, Object>>) backupData.get("encounters"), EncounterRecord.class));
        result.put("triages", restoreList(triageRepo, (List<Map<String, Object>>) backupData.get("triages"), TriageRecord.class));
        result.put("emergency_cases", restoreList(emergencyRepo, (List<Map<String, Object>>) backupData.get("emergency_cases"), EmergencyCase.class));
        result.put("appointments", restoreList(appointmentRepo, (List<Map<String, Object>>) backupData.get("appointments"), Appointment.class));
        result.put("clinical_forms", restoreList(clinicalFormRepo, (List<Map<String, Object>>) backupData.get("clinical_forms"), ClinicalFormRecord.class));
        result.put("prescriptions", restoreList(prescriptionRepo, (List<Map<String, Object>>) backupData.get("prescriptions"), Prescription.class));
        result.put("admissions", restoreList(admissionRepo, (List<Map<String, Object>>) backupData.get("admissions"), Admission.class));
        result.put("lab_tests", restoreList(labTestRepo, (List<Map<String, Object>>) backupData.get("lab_tests"), LabTest.class));
        result.put("imaging", restoreList(imagingRepo, (List<Map<String, Object>>) backupData.get("imaging_requests"), ImagingRequest.class));
        result.put("billing", restoreList(billingRepo, (List<Map<String, Object>>) backupData.get("billing_invoices"), BillingInvoice.class));
        result.put("inventory", restoreList(inventoryRepo, (List<Map<String, Object>>) backupData.get("inventory_records"), InventoryRecord.class));
        result.put("drugs", restoreList(drugRepo, (List<Map<String, Object>>) backupData.get("drugs"), Drug.class));
        result.put("suppliers", restoreList(supplierRepo, (List<Map<String, Object>>) backupData.get("suppliers"), Supplier.class));
        result.put("referrals", restoreList(referralRepo, (List<Map<String, Object>>) backupData.get("referrals"), ReferralRecord.class));
        result.put("insurance_claims", restoreList(claimRepo, (List<Map<String, Object>>) backupData.get("insurance_claims"), InsuranceClaim.class));
        result.put("blood_units", restoreList(bloodUnitRepo, (List<Map<String, Object>>) backupData.get("blood_units"), BloodUnit.class));
        result.put("donations", restoreList(donationRepo, (List<Map<String, Object>>) backupData.get("donations"), Donation.class));
        result.put("queue", restoreList(queueRepo, (List<Map<String, Object>>) backupData.get("queue_tickets"), QueueTicket.class));
        result.put("notifications", restoreList(notificationRepo, (List<Map<String, Object>>) backupData.get("notifications"), NotificationItem.class));
        result.put("attendance", restoreList(attendanceRepo, (List<Map<String, Object>>) backupData.get("attendance_records"), AttendanceRecord.class));
        result.put("staff_schedules", restoreList(staffScheduleRepo, (List<Map<String, Object>>) backupData.get("staff_schedules"), StaffSchedule.class));
        result.put("wards", restoreList(wardRepo, (List<Map<String, Object>>) backupData.get("ward_statuses"), WardStatus.class));
        result.put("audit_logs", restoreList(auditLogRepo, (List<Map<String, Object>>) backupData.get("audit_logs"), AuditLogEntry.class));

        return result;
    }

    private SystemSettings restoreSettings(List<Map<String, Object>> settingsList) {
        if (settingsList == null || settingsList.isEmpty()) {
            return null;
        }
        SystemSettings settings = objectMapper.convertValue(settingsList.get(0), SystemSettings.class);
        settings.setId(1L);
        return settingsRepo.save(settings);
    }

    private <T> String restoreList(JpaRepository<T, ?> repo, List<Map<String, Object>> dataList, Class<T> entityClass) {
        if (dataList == null || dataList.isEmpty()) {
            return "0 restored";
        }
        List<T> entities = new ArrayList<>();
        for (Map<String, Object> data : dataList) {
            data.remove("id");
            try {
                T entity = objectMapper.convertValue(data, entityClass);
                entities.add(entity);
            } catch (Exception e) {
                System.err.println("Failed to convert: " + e.getMessage());
            }
        }
        List<T> saved = repo.saveAll(entities);
        return saved.size() + " restored";
    }
}
