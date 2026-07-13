package com.unza.clinic.service;

import com.unza.clinic.model.*;
import com.unza.clinic.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        if (shouldSeedDemoData()) seed();
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

    private void seed() {
        Patient p1 = new Patient();
        p1.setPatientId("UNZA-2024-001");
        p1.setClinicNumber("UNZA-2024-001");
        p1.setPatientType("STUDENT");
        p1.setName("Mwansa Chanda");
        p1.setAge(22);
        p1.setGender("Male");
        p1.setDob("2004-02-15");
        p1.setPhone("+260 97 1234567");
        p1.setEmail("mwansa@unza.zm");
        p1.setAddress("Plot 12, Roma, Lusaka");
        p1.setBloodGroup("O+");
        p1.setStudentId("CS2021-0045");
        p1.setManNumber("");
        p1.setProgram("BSc Computer Science");
        p1.setSchool("School of Natural Sciences");
        p1.setYear(3);
        p1.setHostel("Africa Hall");
        p1.setEmergencyContact("Grace Chanda");
        p1.setEmergencyPhone("+260 96 9876543");
        p1.setEmergencyRelation("Mother");
        p1.setAllergies("Penicillin");
        p1.setConditions("Asthma");
        p1.setInsurance("NHIMA");
        p1.setStatus("active");
        patientRepo.save(p1);

        Patient p2 = new Patient();
        p2.setPatientId("UNZA-2024-002");
        p2.setClinicNumber("UNZA-2024-002");
        p2.setPatientType("STUDENT");
        p2.setName("Bwalya Mutale");
        p2.setAge(19);
        p2.setGender("Female");
        p2.setDob("2007-06-20");
        p2.setPhone("+260 96 2345678");
        p2.setEmail("bwalya@unza.zm");
        p2.setAddress("Great East Rd, Lusaka");
        p2.setBloodGroup("A+");
        p2.setStudentId("ENG2022-0112");
        p2.setManNumber("");
        p2.setProgram("BSc Civil Engineering");
        p2.setSchool("School of Engineering");
        p2.setYear(2);
        p2.setHostel("New Residence");
        p2.setEmergencyContact("Peter Mutale");
        p2.setEmergencyPhone("+260 97 1112233");
        p2.setEmergencyRelation("Father");
        p2.setAllergies("None");
        p2.setConditions("None");
        p2.setInsurance("NHIMA");
        p2.setStatus("active");
        patientRepo.save(p2);

        StaffMember s1 = new StaffMember();
        s1.setStaffId("STF-001");
        s1.setManNumber("MAN-001");
        s1.setName("Dr. Joseph Tembo");
        s1.setRole("Doctor");
        s1.setDepartment("General Medicine");
        s1.setPhone("+260 97 1111111");
        s1.setEmail("j.tembo@unza.zm");
        s1.setSpecialization("General Practice");
        s1.setStatus("active");
        staffRepo.save(s1);

        StaffMember s2 = new StaffMember();
        s2.setStaffId("STF-002");
        s2.setManNumber("MAN-002");
        s2.setName("Dr. Grace Ng'andu");
        s2.setRole("Doctor");
        s2.setDepartment("Orthopedics");
        s2.setPhone("+260 97 2222222");
        s2.setEmail("g.ngandu@unza.zm");
        s2.setSpecialization("Orthopedics");
        s2.setStatus("active");
        staffRepo.save(s2);

        StaffMember s3 = new StaffMember();
        s3.setStaffId("STF-003");
        s3.setManNumber("MAN-003");
        s3.setName("Nurse Agnes Zulu");
        s3.setRole("Nurse");
        s3.setDepartment("Emergency");
        s3.setPhone("+260 97 3333333");
        s3.setEmail("a.zulu@unza.zm");
        s3.setSpecialization("Emergency Care");
        s3.setStatus("active");
        staffRepo.save(s3);

        Department d1 = new Department();
        d1.setCode("DEPT-GM");
        d1.setName("General Medicine");
        d1.setHead("Dr. Joseph Tembo");
        d1.setDoctors(8);
        d1.setNurses(12);
        d1.setBeds(30);
        d1.setLocation("Block A, Floor 1");
        d1.setPhone("Ext. 101");
        d1.setStatus("Active");
        departmentRepo.save(d1);

        Department d2 = new Department();
        d2.setCode("DEPT-OR");
        d2.setName("Orthopedics");
        d2.setHead("Dr. Grace Ng'andu");
        d2.setDoctors(4);
        d2.setNurses(6);
        d2.setBeds(20);
        d2.setLocation("Block B, Floor 2");
        d2.setPhone("Ext. 201");
        d2.setStatus("Active");
        departmentRepo.save(d2);

        Appointment a1 = new Appointment();
        a1.setAppointmentId("APT-20260408-001");
        a1.setPatientId("UNZA-2024-001");
        a1.setPatientName("Mwansa Chanda");
        a1.setDoctorId("STF-001");
        a1.setDoctorName("Dr. Joseph Tembo");
        a1.setDepartment("General Medicine");
        a1.setDate(LocalDate.now().toString());
        a1.setTime("08:30");
        a1.setType("Consultation");
        a1.setStatus("scheduled");
        a1.setNotes("Follow-up review");
        appointmentRepo.save(a1);

        BillingInvoice inv1 = new BillingInvoice();
        inv1.setInvoiceId("INV-2026-001");
        inv1.setPatientId("UNZA-2024-001");
        inv1.setPatientName("Mwansa Chanda");
        inv1.setItems("General Consultation, Complete Blood Count (CBC), Amoxicillin 500mg x 21, Paracetamol 500mg x 10");
        inv1.setLineItemsJson("""
                [
                  {"tariff_code":"OPD-001","service_name":"Consultation","department":"OPD","quantity":1,"unit_price":200.0,"line_total":200.0},
                  {"tariff_code":"LAB-002","service_name":"Full Blood Count (FBC)","department":"Laboratory","quantity":1,"unit_price":50.0,"line_total":50.0},
                  {"tariff_code":"MED-004","service_name":"Amoxicillin 250mg Capsules","department":"Pharmacy","quantity":1,"unit_price":72.0,"line_total":72.0},
                  {"tariff_code":"MED-001","service_name":"Paracetamol 500mg Tablets","department":"Pharmacy","quantity":1,"unit_price":28.0,"line_total":28.0}
                ]
                """);
        inv1.setSubtotal(350.0);
        inv1.setTax(0.0);
        inv1.setTotal(350.0);
        inv1.setStatus("completed");
        inv1.setDueDate(LocalDate.now().plusDays(7).toString());
        inv1.setPaidDate(LocalDate.now().toString());
        inv1.setPaymentMethod("Mobile Money");
        billingRepo.save(inv1);

        EncounterRecord encounter1 = new EncounterRecord();
        encounter1.setEncounterId("ENC-" + LocalDate.now().getYear() + "-001");
        encounter1.setPatientId("UNZA-2024-001");
        encounter1.setPatientName("Mwansa Chanda");
        encounter1.setPatientType("STUDENT");
        encounter1.setCurrentStage("PHARMACY");
        encounter1.setPaymentStatus("PENDING");
        encounter1.setCheckoutEligible(false);
        encounter1.setCheckedOut(false);
        encounter1.setCreatedAt(LocalDate.now().atTime(8, 0).toString());
        encounter1.setUpdatedAt(LocalDate.now().atTime(10, 15).toString());
        encounter1.setCreatedBy("Front Desk");
        encounter1.setStageHistory(String.join("||",
                LocalDate.now().atTime(8, 0) + "|RECEPTION|Front Desk|Checked in and file opened",
                LocalDate.now().atTime(8, 20) + "|TRIAGE|Nurse Agnes Zulu|Vitals captured and triaged",
                LocalDate.now().atTime(9, 0) + "|CONSULTATION|Dr. Joseph Tembo|Consultation completed",
                LocalDate.now().atTime(9, 30) + "|LABORATORY|Lab Desk|CBC requested",
                LocalDate.now().atTime(10, 0) + "|PHARMACY|Pharmacy Desk|Prescription awaiting dispensing"
        ));
        encounter1.setPendingActions("Dispense medication,Clear invoice,Checkout patient");
        encounter1.setCompletedActions("Reception registration,Triage assessment,OPD consultation,Lab request");
        encounter1.setNotes("Student follow-up visit with lab and pharmacy requirements.");
        encounterRepo.save(encounter1);

        EncounterRecord encounter2 = new EncounterRecord();
        encounter2.setEncounterId("ENC-" + LocalDate.now().getYear() + "-002");
        encounter2.setPatientId("UNZA-2024-002");
        encounter2.setPatientName("Bwalya Mutale");
        encounter2.setPatientType("FIRST_TIME_STUDENT");
        encounter2.setCurrentStage("MCH");
        encounter2.setPaymentStatus("NOT_REQUIRED");
        encounter2.setCheckoutEligible(false);
        encounter2.setCheckedOut(false);
        encounter2.setCreatedAt(LocalDate.now().atTime(7, 45).toString());
        encounter2.setUpdatedAt(LocalDate.now().atTime(9, 10).toString());
        encounter2.setCreatedBy("Reception Clerk");
        encounter2.setStageHistory(String.join("||",
                LocalDate.now().atTime(7, 45) + "|RECEPTION|Reception Clerk|First-time medical examination opened",
                LocalDate.now().atTime(8, 5) + "|TRIAGE|Nurse Agnes Zulu|Height, weight, BMI and vitals started",
                LocalDate.now().atTime(8, 50) + "|CONSULTATION|Dr. Joseph Tembo|Medical examination reviewed",
                LocalDate.now().atTime(9, 10) + "|MCH|Clinic Desk|Awaiting certificate clearance"
        ));
        encounter2.setPendingActions("Finalize exam certificate,Assign permanent clinic number");
        encounter2.setCompletedActions("Registration,Triage,Consultation");
        encounter2.setNotes("First timer student medical examination workflow.");
        encounterRepo.save(encounter2);

        AppUser u1 = new AppUser();
        u1.setUserId("USR-001");
        u1.setName("Admin User");
        u1.setEmail("admin@unza.zm");
        u1.setRole("Admin");
        u1.setDepartment("IT");
        u1.setStaffId("");
        u1.setManNumber("");
        u1.setPassword(passwordEncoder.encode("admin123"));
        u1.setStatus("active");
        u1.setLastLogin("");
        userRepo.save(u1);

        AppUser u2 = new AppUser();
        u2.setUserId("USR-002");
        u2.setName("Dr. Joseph Tembo");
        u2.setEmail("j.tembo@unza.zm");
        u2.setRole("Doctor");
        u2.setDepartment("General Medicine");
        u2.setStaffId("STF-001");
        u2.setManNumber("MAN-001");
        u2.setPassword(passwordEncoder.encode("doctor123"));
        u2.setStatus("active");
        u2.setLastLogin("");
        userRepo.save(u2);

        AppUser u3 = new AppUser();
        u3.setUserId("USR-003");
        u3.setName("Nurse Agnes Zulu");
        u3.setEmail("a.zulu@unza.zm");
        u3.setRole("Nurse");
        u3.setDepartment("Emergency");
        u3.setStaffId("STF-003");
        u3.setManNumber("MAN-003");
        u3.setPassword(passwordEncoder.encode("nurse123"));
        u3.setStatus("active");
        u3.setLastLogin("");
        userRepo.save(u3);

        Drug drg1 = new Drug();
        drg1.setDrugId("DRG-001");
        drg1.setName("Amoxicillin 500mg");
        drg1.setCategory("Antibiotic");
        drg1.setDrugType("Essential Drugs");
        drg1.setBatchNumber("AMX-2026-01");
        drg1.setStock(1240);
        drg1.setReorderLevel(150);
        drg1.setUnit("Tablets");
        drg1.setExpiry("2027-06-15");
        drg1.setStorageLocation("Main Pharmacy Shelf A");
        drg1.setStatus("available");
        drugRepo.save(drg1);

        Drug drg2 = new Drug();
        drg2.setDrugId("DRG-002");
        drg2.setName("Paracetamol 500mg");
        drg2.setCategory("Analgesic");
        drg2.setDrugType("Essential Drugs");
        drg2.setBatchNumber("PCM-2026-03");
        drg2.setStock(3500);
        drg2.setReorderLevel(250);
        drg2.setUnit("Tablets");
        drg2.setExpiry("2027-12-01");
        drg2.setStorageLocation("Main Pharmacy Shelf B");
        drg2.setStatus("available");
        drugRepo.save(drg2);

        Drug drg3 = new Drug();
        drg3.setDrugId("DRG-003");
        drg3.setName("Tenofovir/Lamivudine/Dolutegravir");
        drg3.setCategory("ART");
        drg3.setDrugType("ART");
        drg3.setBatchNumber("TLD-2026-04");
        drg3.setStock(680);
        drg3.setReorderLevel(120);
        drg3.setUnit("Bottles");
        drg3.setExpiry("2027-09-20");
        drg3.setStorageLocation("ART Secure Cabinet");
        drg3.setStatus("available");
        drugRepo.save(drg3);

        Drug drg4 = new Drug();
        drg4.setDrugId("DRG-004");
        drg4.setName("Xylene Reagent");
        drg4.setCategory("Laboratory Chemical");
        drg4.setDrugType("Laboratory Chemicals");
        drg4.setBatchNumber("XYL-2026-02");
        drg4.setStock(34);
        drg4.setReorderLevel(20);
        drg4.setUnit("Bottles");
        drg4.setExpiry("2026-11-10");
        drg4.setStorageLocation("Lab Chemical Store");
        drg4.setStatus("available");
        drugRepo.save(drg4);

        WardStatus w1 = new WardStatus();
        w1.setName("Male Ward A");
        w1.setTotalBeds(30);
        w1.setOccupied(24);
        w1.setAvailable(6);
        wardRepo.save(w1);

        WardStatus w2 = new WardStatus();
        w2.setName("Female Ward B");
        w2.setTotalBeds(30);
        w2.setOccupied(28);
        w2.setAvailable(2);
        wardRepo.save(w2);

        WardStatus w3 = new WardStatus();
        w3.setName("ICU");
        w3.setTotalBeds(10);
        w3.setOccupied(8);
        w3.setAvailable(2);
        wardRepo.save(w3);

        BloodUnit b1 = new BloodUnit();
        b1.setUnitId("BLD-001");
        b1.setBloodType("O+");
        b1.setQuantity(15);
        b1.setStatus("available");
        b1.setExpiryDate("2026-05-15");
        b1.setDonorName("John Banda");
        b1.setCollectionDate("2026-03-15");
        bloodUnitRepo.save(b1);

        BloodUnit b2 = new BloodUnit();
        b2.setUnitId("BLD-002");
        b2.setBloodType("A+");
        b2.setQuantity(12);
        b2.setStatus("available");
        b2.setExpiryDate("2026-05-20");
        b2.setDonorName("Peter Mwansa");
        b2.setCollectionDate("2026-03-20");
        bloodUnitRepo.save(b2);

        AttendanceRecord attendance1 = new AttendanceRecord();
        attendance1.setStaffId("STF-001");
        attendance1.setName("Dr. Joseph Tembo");
        attendance1.setRole("Doctor");
        attendance1.setDepartment("General Medicine");
        attendance1.setShift("Morning");
        attendance1.setCheckIn("07:58");
        attendance1.setCheckOut("");
        attendance1.setStatus("on-duty");
        attendance1.setDate(LocalDate.now().toString());
        attendanceRepo.save(attendance1);

        AttendanceRecord attendance2 = new AttendanceRecord();
        attendance2.setStaffId("STF-002");
        attendance2.setName("Dr. Grace Ng'andu");
        attendance2.setRole("Doctor");
        attendance2.setDepartment("Orthopedics");
        attendance2.setShift("Morning");
        attendance2.setCheckIn("08:14");
        attendance2.setCheckOut("");
        attendance2.setStatus("late");
        attendance2.setDate(LocalDate.now().toString());
        attendanceRepo.save(attendance2);

        AttendanceRecord attendance3 = new AttendanceRecord();
        attendance3.setStaffId("STF-003");
        attendance3.setName("Nurse Agnes Zulu");
        attendance3.setRole("Nurse");
        attendance3.setDepartment("Emergency");
        attendance3.setShift("Night");
        attendance3.setCheckIn("19:00");
        attendance3.setCheckOut("07:02");
        attendance3.setStatus("off-duty");
        attendance3.setDate(LocalDate.now().toString());
        attendanceRepo.save(attendance3);

        StaffSchedule schedule1 = new StaffSchedule();
        schedule1.setScheduleId("SCH-001");
        schedule1.setStaffId("STF-001");
        schedule1.setName("Dr. Joseph Tembo");
        schedule1.setRole("Doctor");
        schedule1.setDepartment("Clinical");
        schedule1.setDayOfWeek("Monday");
        schedule1.setShiftName("Morning OPD");
        schedule1.setStartTime("08:00");
        schedule1.setEndTime("13:00");
        schedule1.setLocation("Consulting Room 1");
        schedule1.setStatus("Scheduled");
        staffScheduleRepo.save(schedule1);

        StaffSchedule schedule2 = new StaffSchedule();
        schedule2.setScheduleId("SCH-002");
        schedule2.setStaffId("STF-003");
        schedule2.setName("Nurse Agnes Zulu");
        schedule2.setRole("Nurse");
        schedule2.setDepartment("Nursing");
        schedule2.setDayOfWeek("Monday");
        schedule2.setShiftName("Triage");
        schedule2.setStartTime("07:30");
        schedule2.setEndTime("16:30");
        schedule2.setLocation("Triage Desk");
        schedule2.setStatus("Scheduled");
        staffScheduleRepo.save(schedule2);

        StaffSchedule schedule3 = new StaffSchedule();
        schedule3.setScheduleId("SCH-003");
        schedule3.setStaffId("STF-002");
        schedule3.setName("Dr. Grace Ng'andu");
        schedule3.setRole("Doctor");
        schedule3.setDepartment("Eye Clinic");
        schedule3.setDayOfWeek("Wednesday");
        schedule3.setShiftName("Specialist Clinic");
        schedule3.setStartTime("09:00");
        schedule3.setEndTime("15:00");
        schedule3.setLocation("Eye Clinic Room");
        schedule3.setStatus("Scheduled");
        staffScheduleRepo.save(schedule3);

        ClinicalFormRecord form1 = new ClinicalFormRecord();
        form1.setFormId("FORM-001");
        form1.setFormType("student_medical_exam");
        form1.setTitle("Student Medical Examination");
        form1.setPatientId("UNZA-2024-002");
        form1.setPatientName("Bwalya Mutale");
        form1.setDepartment("Clinical");
        form1.setEncounterId("ENC-" + LocalDate.now().getYear() + "-002");
        form1.setStatus("completed");
        form1.setCreatedBy("Dr. Joseph Tembo");
        form1.setCreatedAt(LocalDate.now().atTime(9, 20).toString());
        form1.setUpdatedAt(LocalDate.now().atTime(9, 25).toString());
        form1.setPayloadJson("{\"height\":\"165 cm\",\"weight\":\"58 kg\",\"bp\":\"118/72\",\"pulse\":\"76\",\"temp\":\"36.7\",\"urine_protein\":\"Negative\",\"urine_sugar\":\"Negative\",\"hb\":\"13.2\",\"conclusion\":\"FIT\"}");
        clinicalFormRepo.save(form1);

        NotificationItem notification1 = new NotificationItem();
        notification1.setType("appointment");
        notification1.setTitle("Upcoming consultation");
        notification1.setMessage("Mwansa Chanda has a consultation scheduled this morning.");
        notification1.setTime(LocalDate.now().toString());
        notification1.setRead(false);
        notificationRepo.save(notification1);

        NotificationItem notification2 = new NotificationItem();
        notification2.setType("inventory");
        notification2.setTitle("Low stock alert");
        notification2.setMessage("Amoxicillin stock should be reviewed before the next clinic session.");
        notification2.setTime(LocalDate.now().toString());
        notification2.setRead(false);
        notificationRepo.save(notification2);

        AuditLogEntry log1 = new AuditLogEntry();
        log1.setTimestamp(LocalDate.now().atTime(8, 0).toString());
        log1.setUser("System Seeder");
        log1.setRole("System");
        log1.setAction("create");
        log1.setDescription("Initial clinic demo data loaded.");
        log1.setIpAddress("127.0.0.1");
        auditLogRepo.save(log1);

        AuditLogEntry log2 = new AuditLogEntry();
        log2.setTimestamp(LocalDate.now().atTime(8, 10).toString());
        log2.setUser("Admin User");
        log2.setRole("Admin");
        log2.setAction("login");
        log2.setDescription("Administrator signed in.");
        log2.setIpAddress("127.0.0.1");
        auditLogRepo.save(log2);
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

    private boolean shouldSeedDemoData() {
        SystemSettings settings = getSettings();
        return !Boolean.TRUE.equals(settings != null ? settings.getDemoDataCleared() : null)
                && (patientRepo.count() == 0 || userRepo.count() == 0);
    }
}
