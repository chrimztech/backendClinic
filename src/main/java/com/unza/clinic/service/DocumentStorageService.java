package com.unza.clinic.service;

import com.unza.clinic.model.PatientDocument;
import com.unza.clinic.repository.PatientDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final PatientDocumentRepository documentRepo;

    @Value("${app.documents.storage-path:./clinic-documents}")
    private String storagePath;

    public DocumentStorageService(PatientDocumentRepository documentRepo) {
        this.documentRepo = documentRepo;
    }

    public PatientDocument store(MultipartFile file, String patientId, String patientName,
                                  String documentType, String description,
                                  String uploadedBy, String encounterId,
                                  String labTestId, String imagingRequestId) throws IOException {
        Path uploadDir = Paths.get(storagePath, patientId);
        Files.createDirectories(uploadDir);

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
        String storedName = UUID.randomUUID() + ext;
        Path destination = uploadDir.resolve(storedName);
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

        PatientDocument doc = new PatientDocument();
        doc.setDocumentId("DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        doc.setPatientId(patientId);
        doc.setPatientName(patientName);
        doc.setDocumentType(documentType);
        doc.setFileName(originalName);
        doc.setFileSize(file.getSize());
        doc.setContentType(file.getContentType());
        doc.setFilePath(destination.toString());
        doc.setDescription(description);
        doc.setUploadedBy(uploadedBy);
        doc.setUploadedAt(LocalDateTime.now());
        doc.setEncounterId(encounterId);
        doc.setLabTestId(labTestId);
        doc.setImagingRequestId(imagingRequestId);
        doc.setStatus("active");
        doc.setCreatedAt(LocalDateTime.now());

        return documentRepo.save(doc);
    }

    public Path resolveFilePath(PatientDocument doc) {
        return Paths.get(doc.getFilePath());
    }

    public void delete(PatientDocument doc) throws IOException {
        Path file = Paths.get(doc.getFilePath());
        Files.deleteIfExists(file);
        doc.setStatus("deleted");
        documentRepo.save(doc);
    }
}
