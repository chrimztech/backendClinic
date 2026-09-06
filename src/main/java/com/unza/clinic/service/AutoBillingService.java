package com.unza.clinic.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unza.clinic.model.BillingInvoice;
import com.unza.clinic.model.EncounterRecord;
import com.unza.clinic.model.NotificationItem;
import com.unza.clinic.model.Patient;
import com.unza.clinic.model.ServiceTariff;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AutoBillingService {

    private final ClinicDataStore dataStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AutoBillingService(ClinicDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public record AutoBillingResult(boolean posted, boolean exempt, double lineTotal, String invoiceId, String payer) {
        static AutoBillingResult notPosted() {
            return new AutoBillingResult(false, false, 0d, null, null);
        }
    }

    /**
     * Posts a charge for a clinical trigger event onto the encounter's running invoice.
     * Never throws — a billing problem (missing tariff, missing encounter) must not block
     * the underlying clinical action; it is logged as a notification instead.
     */
    public AutoBillingResult postCharge(EncounterRecord encounter, Patient patient, String tariffCode,
                                         int quantity, String triggerLabel, boolean dedupe) {
        if (encounter == null || tariffCode == null || tariffCode.isBlank()) {
            return AutoBillingResult.notPosted();
        }
        ServiceTariff tariff = dataStore.getServiceTariffByCode(tariffCode);
        if (tariff == null || "inactive".equalsIgnoreCase(tariff.getStatus())) {
            warnMissingTariff(tariffCode, triggerLabel);
            return AutoBillingResult.notPosted();
        }

        boolean exempt = isExempt(patient, tariff);
        String payer = resolvePayer(patient);
        int qty = Math.max(1, quantity);
        double unitPrice = exempt ? 0d : orZero(tariff.getPrice());
        double lineTotal = qty * unitPrice;

        if (dedupe) {
            BillingInvoice alreadyPosted = dataStore.getBillingInvoices().stream()
                    .filter(inv -> Objects.equals(inv.getEncounterId(), encounter.getId()))
                    .filter(inv -> !"cancelled".equalsIgnoreCase(inv.getStatus()))
                    .filter(inv -> parseLineItems(inv.getLineItemsJson()).stream()
                            .anyMatch(item -> tariffCode.equalsIgnoreCase(String.valueOf(item.get("tariff_code")))))
                    .findFirst().orElse(null);
            if (alreadyPosted != null) {
                return new AutoBillingResult(false, exempt, 0d, alreadyPosted.getInvoiceId(), payer);
            }
        }

        BillingInvoice invoice = findOrCreateOpenInvoice(encounter);
        List<Map<String, Object>> lineItems = parseLineItems(invoice.getLineItemsJson());

        Map<String, Object> line = new LinkedHashMap<>();
        line.put("tariff_code", tariff.getTariffCode());
        line.put("service_name", exempt ? tariff.getServiceName() + " (Exempt)" : tariff.getServiceName());
        line.put("department", tariff.getDepartment());
        line.put("quantity", qty);
        line.put("unit_price", unitPrice);
        line.put("line_total", lineTotal);
        lineItems.add(line);

        double subtotal = lineItems.stream().mapToDouble(item -> toDouble(item.get("line_total"))).sum();
        invoice.setLineItemsJson(writeJson(lineItems));
        invoice.setItems(lineItems.stream()
                .map(item -> String.valueOf(item.get("service_name")))
                .collect(Collectors.joining(", ")));
        invoice.setSubtotal(subtotal);
        invoice.setTotal(subtotal + orZero(invoice.getTax()));
        if ((invoice.getPaymentMethod() == null || invoice.getPaymentMethod().isBlank())) {
            invoice.setPaymentMethod(payer);
        }
        if (invoice.getTotal() <= 0d) {
            invoice.setStatus("completed");
            invoice.setPaidDate(LocalDateTime.now().toLocalDate().toString());
            invoice.setPaymentMethod("Exempt");
        }
        invoice = dataStore.addBillingInvoice(invoice);

        return new AutoBillingResult(true, exempt, lineTotal, invoice.getInvoiceId(), payer);
    }

    private BillingInvoice findOrCreateOpenInvoice(EncounterRecord encounter) {
        BillingInvoice existing = dataStore.getBillingInvoices().stream()
                .filter(inv -> Objects.equals(inv.getEncounterId(), encounter.getId()))
                .filter(inv -> "pending".equalsIgnoreCase(inv.getStatus()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        BillingInvoice invoice = new BillingInvoice();
        invoice.setInvoiceId("INV-" + LocalDateTime.now().getYear() + "-"
                + String.format("%03d", dataStore.getBillingInvoices().size() + 1));
        invoice.setEncounterId(encounter.getId());
        invoice.setPatientId(encounter.getPatientId());
        invoice.setPatientName(encounter.getPatientName());
        invoice.setItems("");
        invoice.setLineItemsJson("[]");
        invoice.setSubtotal(0d);
        invoice.setTax(0d);
        invoice.setTotal(0d);
        invoice.setStatus("pending");
        invoice.setDueDate("");
        invoice.setPaidDate("");
        invoice.setPaymentMethod("");
        return dataStore.addBillingInvoice(invoice);
    }

    private boolean isExempt(Patient patient, ServiceTariff tariff) {
        if (Boolean.TRUE.equals(tariff.getAlwaysBillable())) {
            return false;
        }
        String type = normalizeType(patient == null ? null : patient.getPatientType());
        return "STUDENT".equals(type) || "FIRST_TIME_STUDENT".equals(type);
    }

    private String resolvePayer(Patient patient) {
        if (patient == null || patient.getInsurance() == null || patient.getInsurance().isBlank()) {
            return "Cash";
        }
        return patient.getInsurance();
    }

    private String normalizeType(String patientType) {
        return (patientType == null || patientType.isBlank()) ? "GENERAL" : patientType.trim().toUpperCase(Locale.ROOT);
    }

    private void warnMissingTariff(String tariffCode, String triggerLabel) {
        NotificationItem notif = new NotificationItem();
        notif.setType("warning");
        notif.setTitle("Billing Tariff Missing");
        notif.setMessage("No active tariff found for code \"" + tariffCode + "\" (" + triggerLabel
                + "). Charge was skipped — add or activate this tariff in Tariff Manager.");
        notif.setTime(LocalDateTime.now().toString());
        notif.setRead(false);
        dataStore.addNotification(notif);
    }

    private List<Map<String, Object>> parseLineItems(String lineItemsJson) {
        if (lineItemsJson == null || lineItemsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, Object>> parsed = objectMapper.readValue(lineItemsJson, new TypeReference<List<Map<String, Object>>>() {});
            return new ArrayList<>(parsed);
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? 0d : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0d;
        }
    }

    private double orZero(Double value) {
        return value == null ? 0d : value;
    }
}
