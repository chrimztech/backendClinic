package com.unza.clinic.service;

import com.unza.clinic.model.*;
import com.unza.clinic.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ClinicDataStore {

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
    private final ServiceTariffRepository serviceTariffRepo;
    private final PasswordEncoder passwordEncoder;

    // Enhanced feature repositories
    private final PatientDocumentRepository patientDocumentRepo;
    private final DrugInteractionRepository drugInteractionRepo;
    private final LabReferenceRangeRepository labReferenceRangeRepo;
    private final RefreshTokenRepository refreshTokenRepo;
    private final VitalAlertRepository vitalAlertRepo;
    private final ControlledDrugRegisterRepository controlledDrugRegisterRepo;
    private final LoginAuditLogRepository loginAuditLogRepo;

    public ClinicDataStore(
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
            ServiceTariffRepository serviceTariffRepo, PasswordEncoder passwordEncoder,
            PatientDocumentRepository patientDocumentRepo, DrugInteractionRepository drugInteractionRepo,
            LabReferenceRangeRepository labReferenceRangeRepo, RefreshTokenRepository refreshTokenRepo,
            VitalAlertRepository vitalAlertRepo, ControlledDrugRegisterRepository controlledDrugRegisterRepo,
            LoginAuditLogRepository loginAuditLogRepo) {
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
        this.serviceTariffRepo = serviceTariffRepo;
        this.passwordEncoder = passwordEncoder;
        this.patientDocumentRepo = patientDocumentRepo;
        this.drugInteractionRepo = drugInteractionRepo;
        this.labReferenceRangeRepo = labReferenceRangeRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.vitalAlertRepo = vitalAlertRepo;
        this.controlledDrugRegisterRepo = controlledDrugRegisterRepo;
        this.loginAuditLogRepo = loginAuditLogRepo;

        ensureSettings();
        ensureTariffs();
    }

    public List<Patient> getPatients() { return patientRepo.findAll(); }
    public Patient getPatientByPatientId(String patientId) { return patientRepo.findByPatientId(patientId).orElse(null); }
    public Patient addPatient(Patient patient) { return patientRepo.save(patient); }
    public Patient updatePatient(Patient patient) { return patientRepo.save(patient); }
    public void deletePatient(Patient patient) { patientRepo.delete(patient); }

    public List<StaffMember> getStaffMembers() { return staffRepo.findAll(); }
    public StaffMember addStaffMember(StaffMember staff) { return staffRepo.save(staff); }
    public StaffMember updateStaffMember(StaffMember staff) { return staffRepo.save(staff); }
    public void deleteStaffMember(StaffMember staff) { staffRepo.delete(staff); }
    public StaffMember getStaffMember(Long id) { return staffRepo.findById(id).orElse(null); }
    public StaffMember getStaffMemberByStaffId(String staffId) { return staffRepo.findByStaffId(staffId).orElse(null); }
    public StaffMember getStaffMemberByManNumber(String manNumber) { return staffRepo.findByManNumber(manNumber).orElse(null); }

    public List<Department> getDepartments() { return departmentRepo.findAll(); }
    public Department addDepartment(Department dept) { return departmentRepo.save(dept); }
    public Department updateDepartment(Department dept) { return departmentRepo.save(dept); }
    public Department getDepartmentByCode(String code) {
        return departmentRepo.findAll().stream()
                .filter(department -> code != null && code.equalsIgnoreCase(department.getCode()))
                .findFirst()
                .orElse(null);
    }

    public List<Appointment> getAppointments() { return appointmentRepo.findAll(); }
    public Appointment addAppointment(Appointment apt) { return appointmentRepo.save(apt); }
    public Appointment updateAppointment(Appointment apt) { return appointmentRepo.save(apt); }

    public List<Prescription> getPrescriptions() { return prescriptionRepo.findAll(); }
    public Prescription addPrescription(Prescription rx) { return prescriptionRepo.save(rx); }
    public Prescription updatePrescription(Prescription rx) { return prescriptionRepo.save(rx); }
    public Prescription getPrescription(Long id) { return prescriptionRepo.findById(id).orElse(null); }

    public List<Admission> getAdmissions() { return admissionRepo.findAll(); }
    public Admission addAdmission(Admission adm) { return admissionRepo.save(adm); }
    public Admission getAdmission(Long id) { return admissionRepo.findById(id).orElse(null); }
    public Admission updateAdmission(Admission admission) { return admissionRepo.save(admission); }

    public List<AppUser> getUsers() { return userRepo.findAll(); }
    public AppUser addUser(AppUser user) {
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            String encoded = isBcryptHash(user.getPassword()) ? user.getPassword() : passwordEncoder.encode(user.getPassword());
            user.setPassword(encoded);
        }
        return userRepo.save(user);
    }
    public AppUser updateUser(AppUser user) {
        AppUser existing = userRepo.findById(user.getId()).orElse(null);
        if (existing != null) {
            // Only update password if a non-blank value is provided
            if (user.getPassword() != null && !user.getPassword().isBlank()) {
                String newPassword = user.getPassword();
                if (!isBcryptHash(newPassword)) {
                    newPassword = passwordEncoder.encode(newPassword);
                }
                user.setPassword(newPassword);
            } else {
                // Preserve existing password
                user.setPassword(existing.getPassword());
            }
            // Preserve other immutable fields if needed
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                user.setEmail(existing.getEmail());
            }
        }
        return userRepo.save(user);
    }
    public void deleteUser(AppUser user) { userRepo.delete(user); }
    public AppUser findUserByEmail(String email) { return userRepo.findByEmailIgnoreCase(email).orElse(null); }
    public AppUser findUserByStaffId(String staffId) {
        return userRepo.findAll().stream()
                .filter(user -> staffId != null && staffId.equalsIgnoreCase(user.getStaffId()))
                .findFirst()
                .orElse(null);
    }
    public AppUser findUserByManNumber(String manNumber) {
        return userRepo.findAll().stream()
                .filter(user -> manNumber != null && manNumber.equalsIgnoreCase(user.getManNumber()))
                .findFirst()
                .orElse(null);
    }

    public List<LabTest> getLabTests() { return labTestRepo.findAll(); }
    public LabTest addLabTest(LabTest test) { return labTestRepo.save(test); }
    public LabTest updateLabTest(LabTest test) { return labTestRepo.save(test); }
    public LabTest getLabTest(Long id) { return labTestRepo.findById(id).orElse(null); }

    public List<BillingInvoice> getBillingInvoices() { return billingRepo.findAll(); }
    public BillingInvoice addBillingInvoice(BillingInvoice inv) { return billingRepo.save(inv); }
    public BillingInvoice updateBillingInvoice(BillingInvoice inv) { return billingRepo.save(inv); }
    public BillingInvoice getBillingInvoiceByInvoiceId(String invoiceId) {
        return billingRepo.findAll().stream()
                .filter(invoice -> invoiceId.equalsIgnoreCase(invoice.getInvoiceId()))
                .findFirst()
                .orElse(null);
    }
    public List<ServiceTariff> getServiceTariffs() { return serviceTariffRepo.findAll(); }
    public ServiceTariff addServiceTariff(ServiceTariff tariff) { return serviceTariffRepo.save(tariff); }
    public ServiceTariff getServiceTariffByCode(String tariffCode) {
        return serviceTariffRepo.findAll().stream()
                .filter(tariff -> tariffCode != null && tariffCode.equalsIgnoreCase(tariff.getTariffCode()))
                .findFirst()
                .orElse(null);
    }

    public List<InventoryRecord> getInventoryRecords() { return inventoryRepo.findAll(); }
    public InventoryRecord addInventoryRecord(InventoryRecord rec) { return inventoryRepo.save(rec); }

    public List<Supplier> getSuppliers() { return supplierRepo.findAll(); }
    public Supplier addSupplier(Supplier sup) { return supplierRepo.save(sup); }

    public List<Drug> getDrugs() { return drugRepo.findAll(); }
    public Drug addDrug(Drug drug) { return drugRepo.save(drug); }
    public Drug updateDrug(Drug drug) { return drugRepo.save(drug); }
    public Drug getDrug(Long id) { return drugRepo.findById(id).orElse(null); }

    public List<ImagingRequest> getImagingRequests() { return imagingRepo.findAll(); }
    public ImagingRequest addImagingRequest(ImagingRequest req) { return imagingRepo.save(req); }
    public ImagingRequest updateImagingRequest(ImagingRequest req) { return imagingRepo.save(req); }

    public List<InsuranceClaim> getInsuranceClaims() { return claimRepo.findAll(); }
    public InsuranceClaim addInsuranceClaim(InsuranceClaim claim) { return claimRepo.save(claim); }

    public List<ReferralRecord> getReferralRecords() { return referralRepo.findAll(); }
    public ReferralRecord addReferralRecord(ReferralRecord ref) { return referralRepo.save(ref); }
    public ReferralRecord updateReferralRecord(ReferralRecord ref) { return referralRepo.save(ref); }

    public List<TriageRecord> getTriageRecords() { return triageRepo.findAll(); }
    public TriageRecord addTriageRecord(TriageRecord tri) { return triageRepo.save(tri); }
    public TriageRecord updateTriageRecord(TriageRecord tri) { return triageRepo.save(tri); }

    public List<QueueTicket> getQueueTickets() { return queueRepo.findAll(); }
    public QueueTicket addQueueTicket(QueueTicket q) { return queueRepo.save(q); }
    public QueueTicket updateQueueTicket(QueueTicket q) { return queueRepo.save(q); }
    public QueueTicket getQueueTicket(Long id) { return queueRepo.findById(id).orElse(null); }
    public void deleteQueueTicket(Long id) { queueRepo.deleteById(id); }

    public List<EmergencyCase> getEmergencyCases() { return emergencyRepo.findAll(); }
    public EmergencyCase addEmergencyCase(EmergencyCase e) { return emergencyRepo.save(e); }
    public EmergencyCase updateEmergencyCase(EmergencyCase e) { return emergencyRepo.save(e); }
    public EmergencyCase getEmergencyCase(Long id) { return emergencyRepo.findById(id).orElse(null); }

    public List<BloodUnit> getBloodUnits() { return bloodUnitRepo.findAll(); }
    public BloodUnit addBloodUnit(BloodUnit b) { return bloodUnitRepo.save(b); }
    public BloodUnit updateBloodUnit(BloodUnit b) { return bloodUnitRepo.save(b); }

    public List<Donation> getDonations() { return donationRepo.findAll(); }
    public Donation addDonation(Donation d) { return donationRepo.save(d); }

    public List<NotificationItem> getNotifications() { return notificationRepo.findAll(); }
    public NotificationItem addNotification(NotificationItem n) { return notificationRepo.save(n); }
    public NotificationItem updateNotification(NotificationItem n) { return notificationRepo.save(n); }
    public NotificationItem getNotification(Long id) { return notificationRepo.findById(id).orElse(null); }
    public void deleteNotification(Long id) { notificationRepo.deleteById(id); }

    public List<AuditLogEntry> getAuditLogs() { return auditLogRepo.findAll(); }
    public AuditLogEntry addAuditLog(AuditLogEntry log) { return auditLogRepo.save(log); }

    public List<AttendanceRecord> getAttendanceRecords() { return attendanceRepo.findAll(); }
    public AttendanceRecord updateAttendanceRecord(AttendanceRecord a) { return attendanceRepo.save(a); }
    public AttendanceRecord getAttendanceRecord(Long id) { return attendanceRepo.findById(id).orElse(null); }
    public List<StaffSchedule> getStaffSchedules() { return staffScheduleRepo.findAll(); }
    public List<StaffSchedule> getStaffSchedulesByWeek(String weekOf) { return staffScheduleRepo.findByWeekOf(weekOf); }
    public StaffSchedule addStaffSchedule(StaffSchedule schedule) { return staffScheduleRepo.save(schedule); }
    public StaffSchedule getStaffSchedule(Long id) { return staffScheduleRepo.findById(id).orElse(null); }
    public void deleteStaffSchedule(Long id) { staffScheduleRepo.deleteById(id); }
    public List<ClinicalFormRecord> getClinicalForms() { return clinicalFormRepo.findAll(); }
    public ClinicalFormRecord addClinicalForm(ClinicalFormRecord form) { return clinicalFormRepo.save(form); }
    public ClinicalFormRecord updateClinicalForm(ClinicalFormRecord form) { return clinicalFormRepo.save(form); }

    // ---------------------------------------------------------------
    // Patient Documents (Feature 2)
    // ---------------------------------------------------------------
    public List<PatientDocument> getDocumentsByPatient(String patientId) { return patientDocumentRepo.findByPatientIdAndStatus(patientId, "active"); }
    public PatientDocument getDocumentById(String documentId) { return patientDocumentRepo.findByDocumentId(documentId).orElse(null); }
    public PatientDocument saveDocument(PatientDocument doc) { return patientDocumentRepo.save(doc); }

    // ---------------------------------------------------------------
    // Drug Interactions (Feature 4)
    // ---------------------------------------------------------------
    public List<DrugInteraction> getDrugInteractions() { return drugInteractionRepo.findAll(); }
    public DrugInteraction getDrugInteraction(String interactionId) { return drugInteractionRepo.findByInteractionId(interactionId).orElse(null); }
    public DrugInteraction saveDrugInteraction(DrugInteraction di) { return drugInteractionRepo.save(di); }
    public void deleteDrugInteraction(DrugInteraction di) { drugInteractionRepo.delete(di); }

    // ---------------------------------------------------------------
    // Lab Reference Ranges (Feature 6)
    // ---------------------------------------------------------------
    public List<LabReferenceRange> getLabReferenceRanges() { return labReferenceRangeRepo.findAll(); }
    public LabReferenceRange getLabReferenceRange(String rangeId) { return labReferenceRangeRepo.findByRangeId(rangeId).orElse(null); }
    public LabReferenceRange saveLabReferenceRange(LabReferenceRange r) { return labReferenceRangeRepo.save(r); }
    public void deleteLabReferenceRange(LabReferenceRange r) { labReferenceRangeRepo.delete(r); }
    public List<LabReferenceRange> findRangesForTest(String testName, String gender, int age) {
        return labReferenceRangeRepo.findApplicableRange(testName, gender, age);
    }

    // ---------------------------------------------------------------
    // Refresh Tokens (Feature 8)
    // ---------------------------------------------------------------
    public RefreshToken saveRefreshToken(RefreshToken rt) { return refreshTokenRepo.save(rt); }
    public RefreshToken getRefreshToken(String tokenId) { return refreshTokenRepo.findByTokenId(tokenId).orElse(null); }
    public int revokeAllRefreshTokens(String userId) { return refreshTokenRepo.revokeAllByUserId(userId); }

    // ---------------------------------------------------------------
    // Vital Alerts (Feature 5)
    // ---------------------------------------------------------------
    public List<VitalAlert> getVitalAlerts() { return vitalAlertRepo.findAll(); }
    public List<VitalAlert> getVitalAlertsByPatient(String patientId) { return vitalAlertRepo.findByPatientIdOrderByCreatedAtDesc(patientId); }
    public List<VitalAlert> getUnacknowledgedAlerts() { return vitalAlertRepo.findByAcknowledged(false); }
    public VitalAlert saveVitalAlert(VitalAlert a) { return vitalAlertRepo.save(a); }
    public VitalAlert getVitalAlert(String alertId) { return vitalAlertRepo.findByAlertId(alertId).orElse(null); }

    // ---------------------------------------------------------------
    // Login Audit Log (item 9)
    // ---------------------------------------------------------------
    public LoginAuditLog saveLoginAuditLog(LoginAuditLog entry) { return loginAuditLogRepo.save(entry); }
    public List<LoginAuditLog> getLoginAuditLogs() { return loginAuditLogRepo.findTop100ByOrderByLoggedAtDesc(); }
    public List<LoginAuditLog> getLoginAuditLogsByEmail(String email) { return loginAuditLogRepo.findByEmailIgnoreCase(email); }

    // ---------------------------------------------------------------
    // Controlled Drug Register (Feature 11)
    // ---------------------------------------------------------------
    public List<ControlledDrugRegister> getControlledDrugRegister() { return controlledDrugRegisterRepo.findAllByOrderByCreatedAtDesc(); }
    public ControlledDrugRegister getControlledDrugEntry(String entryId) { return controlledDrugRegisterRepo.findByEntryId(entryId).orElse(null); }
    public ControlledDrugRegister saveControlledDrugEntry(ControlledDrugRegister entry) { return controlledDrugRegisterRepo.save(entry); }
    public List<ControlledDrugRegister> getControlledDrugEntriesByDrug(String drugName) { return controlledDrugRegisterRepo.findByDrugNameIgnoreCase(drugName); }

    public boolean verifyPassword(AppUser user, String rawPassword) {
        if (user == null || rawPassword == null) {
            return false;
        }

        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        boolean matches = rawPassword.equals(storedPassword);
        if (matches) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepo.save(user);
        }
        return matches;
    }

    private boolean isBcryptHash(String password) {
        return password != null
                && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    public List<WardStatus> getWards() { return wardRepo.findAll(); }
    public WardStatus getWard(Long id) { return wardRepo.findById(id).orElse(null); }
    public WardStatus addWard(WardStatus ward) { return wardRepo.save(ward); }
    public WardStatus updateWard(WardStatus ward) { return wardRepo.save(ward); }
    public WardStatus addBed(Long wardId) {
        WardStatus ward = wardRepo.findById(wardId).orElse(null);
        if (ward == null) return null;
        ward.setTotalBeds(ward.getTotalBeds() + 1);
        return wardRepo.save(ward);
    }
    public WardStatus removeBed(Long wardId) {
        WardStatus ward = wardRepo.findById(wardId).orElse(null);
        if (ward == null) return null;
        if (ward.getTotalBeds() > ward.getOccupied()) {
            ward.setTotalBeds(ward.getTotalBeds() - 1);
            return wardRepo.save(ward);
        }
        return ward;
    }
    public WardStatus updateWardBeds(Long wardId, int totalBeds) {
        WardStatus ward = wardRepo.findById(wardId).orElse(null);
        if (ward == null) return null;
        if (totalBeds < ward.getOccupied()) return ward;
        ward.setTotalBeds(totalBeds);
        return wardRepo.save(ward);
    }
    public SystemSettings getSettings() { return settingsRepo.findById(1L).orElse(null); }
    public SystemSettings saveSettings(SystemSettings settings) { return settingsRepo.save(settings); }
    public AppUser getUser(Long id) { return userRepo.findById(id).orElse(null); }
    public List<EncounterRecord> getEncounterRecords() { return encounterRepo.findAll(); }
    public EncounterRecord addEncounterRecord(EncounterRecord record) { return encounterRepo.save(record); }
    public EncounterRecord updateEncounterRecord(EncounterRecord record) { return encounterRepo.save(record); }
    public EncounterRecord getEncounterRecord(Long id) { return encounterRepo.findById(id).orElse(null); }

    public Map<String, Integer> clearSeededData(Long preserveUserId) {
        Map<String, Integer> summary = new LinkedHashMap<>();

        summary.put("clinical_forms", clinicalFormRepo.findAll().size());
        clinicalFormRepo.deleteAllInBatch();
        summary.put("encounters", encounterRepo.findAll().size());
        encounterRepo.deleteAllInBatch();
        summary.put("appointments", appointmentRepo.findAll().size());
        appointmentRepo.deleteAllInBatch();
        summary.put("prescriptions", prescriptionRepo.findAll().size());
        prescriptionRepo.deleteAllInBatch();
        summary.put("lab_tests", labTestRepo.findAll().size());
        labTestRepo.deleteAllInBatch();
        summary.put("billing_invoices", billingRepo.findAll().size());
        billingRepo.deleteAllInBatch();
        summary.put("admissions", admissionRepo.findAll().size());
        admissionRepo.deleteAllInBatch();
        summary.put("imaging_requests", imagingRepo.findAll().size());
        imagingRepo.deleteAllInBatch();
        summary.put("insurance_claims", claimRepo.findAll().size());
        claimRepo.deleteAllInBatch();
        summary.put("referrals", referralRepo.findAll().size());
        referralRepo.deleteAllInBatch();
        summary.put("triage_records", triageRepo.findAll().size());
        triageRepo.deleteAllInBatch();
        summary.put("queue_tickets", queueRepo.findAll().size());
        queueRepo.deleteAllInBatch();
        summary.put("emergency_cases", emergencyRepo.findAll().size());
        emergencyRepo.deleteAllInBatch();
        summary.put("blood_units", bloodUnitRepo.findAll().size());
        bloodUnitRepo.deleteAllInBatch();
        summary.put("donations", donationRepo.findAll().size());
        donationRepo.deleteAllInBatch();
        summary.put("attendance_records", attendanceRepo.findAll().size());
        attendanceRepo.deleteAllInBatch();
        summary.put("staff_schedules", staffScheduleRepo.findAll().size());
        staffScheduleRepo.deleteAllInBatch();
        summary.put("inventory_records", inventoryRepo.findAll().size());
        inventoryRepo.deleteAllInBatch();
        summary.put("suppliers", supplierRepo.findAll().size());
        supplierRepo.deleteAllInBatch();
        summary.put("drugs", drugRepo.findAll().size());
        drugRepo.deleteAllInBatch();
        summary.put("wards", wardRepo.findAll().size());
        wardRepo.deleteAllInBatch();
        summary.put("notifications", notificationRepo.findAll().size());
        notificationRepo.deleteAllInBatch();
        summary.put("audit_logs", auditLogRepo.findAll().size());
        auditLogRepo.deleteAllInBatch();
        summary.put("patients", patientRepo.findAll().size());
        patientRepo.deleteAllInBatch();
        summary.put("staff_members", staffRepo.findAll().size());
        staffRepo.deleteAllInBatch();
        summary.put("departments", departmentRepo.findAll().size());
        departmentRepo.deleteAllInBatch();

        List<AppUser> removableUsers = userRepo.findAll().stream()
                .filter(user -> preserveUserId == null || !Objects.equals(user.getId(), preserveUserId))
                .toList();
        summary.put("users_removed", removableUsers.size());
        if (!removableUsers.isEmpty()) {
            userRepo.deleteAllInBatch(removableUsers);
        }

        SystemSettings settings = getSettings();
        if (settings != null) {
            settings.setDemoDataCleared(true);
            saveSettings(settings);
        }

        return summary;
    }

    private void ensureSettings() {
        if (settingsRepo.count() > 0) {
            SystemSettings existing = settingsRepo.findById(1L).orElse(null);
            if (existing != null) {
                if (existing.getBackupEnabled() == null) existing.setBackupEnabled(true);
                if (existing.getBackupFrequency() == null || existing.getBackupFrequency().isBlank()) existing.setBackupFrequency("Daily");
                if (existing.getBackupLocation() == null || existing.getBackupLocation().isBlank()) existing.setBackupLocation("Local secure server");
                if (existing.getLastBackupAt() == null || existing.getLastBackupAt().isBlank()) existing.setLastBackupAt(LocalDate.now().atStartOfDay().toString());
                if (existing.getDemoDataCleared() == null) existing.setDemoDataCleared(false);
                settingsRepo.save(existing);
            }
            return;
        }

        SystemSettings settings = new SystemSettings();
        settings.setId(1L);
        settings.setHospitalName("University of Zambia Clinic");
        settings.setContactPhone("+260 211 290000");
        settings.setAddress("Great East Road, Lusaka, Zambia");
        settings.setEmailNotifications(true);
        settings.setSmsNotifications(true);
        settings.setLowStockAlerts(true);
        settings.setTwoFactorAuth(true);
        settings.setAuditLogging(true);
        settings.setAutoLogout(true);
        settings.setBackupEnabled(true);
        settings.setBackupFrequency("Daily");
        settings.setBackupLocation("Local secure server");
        settings.setLastBackupAt(LocalDate.now().atStartOfDay().toString());
        settings.setDemoDataCleared(false);
        settingsRepo.save(settings);
    }

    private void ensureTariffs() {
        if (serviceTariffRepo.count() > 0) {
            return;
        }

        addTariff("MCH-001", "MCH", "Maternal & Child Health", "Antenatal booking", "per visit", 800);
        addTariff("MCH-002", "MCH", "Maternal & Child Health", "Antenatal re-attendance", "per visit", 200);
        addTariff("MCH-003", "MCH", "Maternal & Child Health", "Family planning/post-natal re-attendance", "per visit", 150);
        addTariff("MCH-004", "MCH", "Maternal & Child Health", "Jadelle or loop insertion", "per procedure", 300);
        addTariff("MCH-005", "MCH", "Maternal & Child Health", "Jadelle or loop removal", "per procedure", 300);
        addTariff("MCH-006", "MCH", "Maternal & Child Health", "Under five clinic attendance", "per visit", 100);

        addTariff("OPD-001", "OPD", "Outpatient Services", "Consultation", "per visit", 200);
        addTariff("OPD-002", "OPD", "Outpatient Services", "Review", "per visit", 120);
        addTariff("OPD-003", "OPD", "Outpatient Services", "Special consultation", "per visit", 250);
        addTariff("OPD-004", "OPD", "Outpatient Services", "Ambulance", "per service", 500);
        addTariff("OPD-005", "OPD", "Outpatient Services", "Ambulance hire", "per service", 1000);
        addTariff("OPD-006", "OPD", "Outpatient Services", "Observation", "per day", 300);
        addTariff("OPD-007", "Inpatient", "Inpatient Services", "Lodging", "per day", 200);
        addTariff("OPD-008", "OPD", "Outpatient Services", "Nursing care OPD", "per day", 100);
        addTariff("OPD-009", "Inpatient", "Inpatient Services", "Nursing care IPD", "per day", 100);
        addTariff("OPD-010", "Inpatient", "Inpatient Services", "Medical care", "per day", 100);
        addTariff("OPD-011", "Inpatient", "Inpatient Services", "Admission deposits", "per admission", 2000);
        addTariff("OPD-012", "OPD", "Procedures", "Injection", "per injection", 20);
        addTariff("OPD-013", "OPD", "Procedures", "Suturing (small)", "per procedure", 100);
        addTariff("OPD-014", "OPD", "Procedures", "Suturing (medium)", "per procedure", 150);
        addTariff("OPD-015", "OPD", "Procedures", "Suturing (large)", "per procedure", 200);
        addTariff("OPD-016", "OPD", "Procedures", "Incision and drainage", "per procedure", 200);
        addTariff("OPD-017", "OPD", "Procedures", "Wound dressing (up to 7 days)", "per case", 200);
        addTariff("OPD-018", "OPD", "Procedures", "Wound dressing (more than 7 days)", "per case", 75);
        addTariff("OPD-019", "OPD", "Procedures", "Removal of stitches", "per procedure", 100);
        addTariff("OPD-020", "OPD", "Procedures", "Ear syringing", "per procedure", 200);
        addTariff("OPD-021", "OPD", "Laboratory", "COVID-19 test PCR", "per test", 800);
        addTariff("OPD-022", "OPD", "Laboratory", "COVID-19 test RDT", "per test", 500);
        addTariff("OPD-023", "OPD", "Laboratory", "COVID-19 RDT", "per test", 200);

        addTariff("LAB-001", "Laboratory", "Blood", "Blood slide for malaria parasites", "per test", 50);
        addTariff("LAB-002", "Laboratory", "Blood", "Full Blood Count (FBC)", "per test", 225);
        addTariff("LAB-003", "Laboratory", "Blood", "RDT for malaria", "per test", 50);
        addTariff("LAB-004", "Laboratory", "Blood", "Erythrocyte Sedimentation Rate (ESR)", "per test", 100);
        addTariff("LAB-005", "Laboratory", "Blood", "Hemoglobin", "per test", 50);
        addTariff("LAB-006", "Laboratory", "Blood", "Bleeding time", "per test", 150);
        addTariff("LAB-007", "Laboratory", "Blood", "Clotting time", "per test", 150);
        addTariff("LAB-008", "Laboratory", "Blood", "Blood group", "per test", 100);
        addTariff("LAB-009", "Laboratory", "Blood", "Sickling test", "per test", 250);
        addTariff("LAB-010", "Laboratory", "Chemistry", "Blood sugar (fasting/random)", "per test", 100);
        addTariff("LAB-011", "Laboratory", "Chemistry", "Liver function test (LFTs)", "per test", 600);
        addTariff("LAB-012", "Laboratory", "Chemistry", "Kidney function test (KFTs)", "per test", 400);
        addTariff("LAB-013", "Laboratory", "Chemistry", "Electrolytes", "per test", 300);
        addTariff("LAB-014", "Laboratory", "Chemistry", "Lipid profile", "per test", 500);
        addTariff("LAB-015", "Laboratory", "Serology", "Rapid Plasma Reagin (RPR)", "per test", 100);
        addTariff("LAB-016", "Laboratory", "Serology", "Hepatitis B or C qualitative", "per test", 225);
        addTariff("LAB-017", "Laboratory", "Serology", "HIV and counselling", "per test", 225);
        addTariff("LAB-018", "Laboratory", "Serology", "Rheumatoid Factor (RF)", "per test", 225);
        addTariff("LAB-019", "Laboratory", "Serology", "Anti Streptolysin O Test (ASOT)", "per test", 225);
        addTariff("LAB-020", "Laboratory", "Chemistry", "Cryptococcal Antigen Test (CAT)", "per test", 300);
        addTariff("LAB-021", "Laboratory", "Chemistry", "Total cholesterol", "per test", 150);
        addTariff("LAB-022", "Laboratory", "Chemistry", "HDL cholesterol", "per test", 150);
        addTariff("LAB-023", "Laboratory", "Chemistry", "LDL cholesterol", "per test", 150);
        addTariff("LAB-024", "Laboratory", "Chemistry", "Amylase", "per test", 200);
        addTariff("LAB-025", "Laboratory", "Chemistry", "Uric acid", "per test", 180);
        addTariff("LAB-026", "Laboratory", "Serology", "Widal test", "per test", 250);
        addTariff("LAB-027", "Laboratory", "Culture", "Blood culture", "per test", 300);
        addTariff("LAB-028", "Laboratory", "Stool", "Stool M/C/S", "per test", 120);
        addTariff("LAB-029", "Laboratory", "Stool", "Occult blood test", "per test", 225);
        addTariff("LAB-030", "Laboratory", "Microbiology", "Culture/Sensitivity", "per test", 300);
        addTariff("LAB-031", "Laboratory", "Microbiology", "Modified ZN stain", "per test", 200);
        addTariff("LAB-032", "Laboratory", "Microbiology", "Formalin-Ether concentration", "per test", 200);
        addTariff("LAB-033", "Laboratory", "Urine", "Urinalysis", "per test", 70);
        addTariff("LAB-034", "Laboratory", "Urine", "Routine microscopy", "per test", 120);
        addTariff("LAB-035", "Laboratory", "Urine", "Culture/Sensitivity (urine)", "per test", 300);
        addTariff("LAB-036", "Laboratory", "Microbiology", "Gram stain", "per test", 150);
        addTariff("LAB-037", "Laboratory", "Microbiology", "Ziehl Nelson stain", "per test", 200);
        addTariff("LAB-038", "Laboratory", "Pregnancy", "Gravindex", "per test", 70);
        addTariff("LAB-039", "Laboratory", "Swabs", "Wet preparation (WP)", "per test", 150);
        addTariff("LAB-040", "Laboratory", "Miscellaneous", "Semen analysis", "per test", 225);
        addTariff("LAB-041", "Laboratory", "Cardiac", "Troponin I", "per test", 250);
        addTariff("LAB-042", "Laboratory", "Serology", "HBSAG (quantitative test)", "per test", 270);
        addTariff("LAB-043", "Laboratory", "Cardiac", "CK-MB", "per test", 250);
        addTariff("LAB-044", "Laboratory", "Cardiac", "Dimer", "per test", 300);
        addTariff("LAB-045", "Laboratory", "Chemistry", "Ferritin", "per test", 300);
        addTariff("LAB-046", "Laboratory", "Chemistry", "T4", "per test", 250);
        addTariff("LAB-047", "Laboratory", "Serology", "H-Pylori (Ag stool)", "per test", 300);
        addTariff("LAB-048", "Laboratory", "Serology", "H-Pylori (blood)", "per test", 200);
        addTariff("LAB-049", "Laboratory", "Cardiac", "Cardiac profile", "per test", 900);
        addTariff("LAB-050", "Laboratory", "Serology", "PSA (quantitative)", "per test", 270);
        addTariff("LAB-051", "Laboratory", "TB", "TB LAM", "per test", 200);
        addTariff("LAB-052", "Laboratory", "RDT", "Chlamydia (RDT)", "per test", 200);
        addTariff("LAB-053", "Laboratory", "RDT", "Gonorrhea (RDT)", "per test", 200);

        addTariff("MED-001", "Pharmacy", "Medicines", "Paracetamol 500mg Tablets", "per 40 tabs", 75);
        addTariff("MED-002", "Pharmacy", "Medicines", "Paracetamol syrup", "per bottle", 24);
        addTariff("MED-003", "Pharmacy", "Medicines", "Acyclovir Cream", "per tube", 60);
        addTariff("MED-004", "Pharmacy", "Medicines", "Amoxicillin 250mg Capsules", "per 30 caps", 72);
        addTariff("MED-005", "Pharmacy", "Medicines", "Amoxicillin 125mg/5ml Suspension", "per 100ml", 60);
        addTariff("MED-006", "Pharmacy", "Medicines", "Amlodipine 10mg Tablets", "per 10 tabs", 78);
        addTariff("MED-007", "Pharmacy", "Medicines", "Ciprofloxacin 500mg Tablets", "per 10 tabs", 48);
        addTariff("MED-008", "Pharmacy", "Medicines", "Diclofenac 50mg Tablets", "per 10 tabs", 36);
        addTariff("MED-009", "Pharmacy", "Medicines", "Doxycycline 100mg Capsules", "per 10 caps", 36);
        addTariff("MED-010", "Pharmacy", "Medicines", "Metronidazole 200mg Tablets", "per 30 tabs", 50);
        addTariff("MED-011", "Pharmacy", "Medicines", "Omeprazole 20mg Capsules", "per 10 caps", 60);
        addTariff("MED-012", "Pharmacy", "Medicines", "Oral Rehydration Salts", "per sachet", 60);
        addTariff("MED-013", "Pharmacy", "Medicines", "Salbutamol inhaler", "per each", 96);
        addTariff("MED-014", "Pharmacy", "Medicines", "Vitamin C Tablets", "per 10 tabs", 72);
        addTariff("MED-015", "Pharmacy", "Medicines", "Promethazine Syrup", "per bottle", 36);
        addTariff("MED-016", "Pharmacy", "Medicines", "Hydrocortisone Cream", "per tube", 60);

        addTariff("EXM-001", "Medical Exams", "Medical Examinations", "Student medical to study in Russia/Algeria/India/others", "per examination", 500);
        addTariff("EXM-002", "Medical Exams", "Medical Examinations", "Students accepted to study in Japan", "per examination", 1000);
        addTariff("EXM-003", "Medical Exams", "Medical Examinations", "Students accepted to study in China", "per examination", 700);
        addTariff("EXM-004", "Medical Exams", "Medical Examinations", "Foreign students accepted to study at UNZA", "per examination", 400);
        addTariff("EXM-005", "Medical Exams", "Medical Examinations", "General medical examination to the public", "per examination", 350);
        addTariff("EXM-006", "Medical Exams", "Medical Examinations", "RTSA medical examinations for drivers", "per examination", 250);
        addTariff("EXM-007", "Medical Exams", "Medical Examinations", "Medical examination for food handlers", "per examination", 675);
        addTariff("EXM-008", "Medical Exams", "Medical Examinations", "Medical examination for UNZA admitted students", "per examination", 250);
        addTariff("EXM-009", "Medical Exams", "Medical Examinations", "Medical reports for NAPSA/insurance claims", "per report", 300);

        addTariff("EYE-001", "Eye Clinic", "Eye Care", "Lens Trans", "per item", 600);
        addTariff("EYE-002", "Eye Clinic", "Eye Care", "Trans Bifocal", "per item", 1300);
        addTariff("EYE-003", "Eye Clinic", "Eye Care", "Trans Progressive", "per item", 1500);
        addTariff("EYE-004", "Eye Clinic", "Eye Care", "Clear ARC", "per item", 240);
        addTariff("EYE-005", "Eye Clinic", "Eye Care", "PGX with ARC", "per item", 350);
        addTariff("EYE-006", "Eye Clinic", "Eye Care", "Fitting fee", "per fitting", 50);
        addTariff("EYE-007", "Eye Clinic", "Frames", "Vision Spring", "per item", 250);
        addTariff("EYE-008", "Eye Clinic", "Glasses", "Reader v/s", "per item", 100);
        addTariff("EYE-009", "Eye Clinic", "Glasses", "Reader v/s bifocal", "per item", 150);
        addTariff("EYE-010", "Eye Clinic", "Glasses", "Old stock", "per item", 150);
    }

    private void addTariff(String tariffCode, String department, String category, String serviceName, String unitLabel, double price) {
        ServiceTariff tariff = new ServiceTariff();
        tariff.setTariffCode(tariffCode);
        tariff.setDepartment(department);
        tariff.setCategory(category);
        tariff.setServiceName(serviceName);
        tariff.setUnitLabel(unitLabel);
        tariff.setPrice(price);
        tariff.setStatus("active");
        serviceTariffRepo.save(tariff);
    }
}
